package com.bookworms.server.reviews

import com.bookworms.server.TestcontainersConfiguration
import com.bookworms.server.books.Book
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.BookSource
import com.bookworms.server.users.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ReviewFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val reviewRepository: BookReviewRepository,
	@Autowired private val bookRepository: BookRepository,
	@Autowired private val userRepository: UserRepository,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var solaris: UUID

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		solaris = bookRepository.save(Book(BookSource.MANUAL, "Solaris", "Stanisław Lem", "9788308049461")).id!!
		agata = registerAndLogIn("Agata", "agata")
		marek = registerAndLogIn("Marek", "marek")
	}

	// The other test classes share this database, and a review keeps its book and author from being deleted.
	@AfterEach
	fun cleanDatabase() {
		reviewRepository.deleteAllInBatch()
		bookRepository.deleteAllInBatch()
		userRepository.deleteAllInBatch()
	}

	@Test
	fun `a book nobody rated has no average, and anyone can see that`() {
		mockMvc.get("/books/$solaris/reviews").andExpect {
			status { isOk() }
			jsonPath("$.average") { doesNotExist() }
			jsonPath("$.count") { value(0) }
			jsonPath("$.myRating") { doesNotExist() }
			jsonPath("$.reviews.length()") { value(0) }
		}
	}

	@Test
	fun `rating needs logging in`() {
		mockMvc.put("/books/$solaris/review") { json("""{"rating": 5}""") }
			.andExpect { status { isUnauthorized() } }
		mockMvc.delete("/books/$solaris/review").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `ratings make the average, and each person sees their own`() {
		rate(agata, """{"rating": 5}""").andExpect {
			status { isOk() }
			jsonPath("$.average") { value(5.0) }
			jsonPath("$.myRating") { value(5) }
		}
		rate(marek, """{"rating": 4, "content": " Slow start, great ending. ", "hasSpoilers": false}""").andExpect {
			status { isOk() }
			jsonPath("$.average") { value(4.5) }
			jsonPath("$.count") { value(2) }
			jsonPath("$.myRating") { value(4) }
			// The newest first.
			jsonPath("$.reviews[0].authorName") { value("Marek") }
			jsonPath("$.reviews[0].authorUsername") { value("marek") }
			jsonPath("$.reviews[0].content") { value("Slow start, great ending.") }
			jsonPath("$.reviews[1].content") { doesNotExist() }
		}

		mockMvc.get("/books/$solaris/reviews") { bearer(agata) }.andExpect {
			jsonPath("$.myRating") { value(5) }
		}
		// Without an account: the same ratings, just no own one.
		mockMvc.get("/books/$solaris/reviews").andExpect {
			jsonPath("$.average") { value(4.5) }
			jsonPath("$.myRating") { doesNotExist() }
		}
	}

	@Test
	fun `rating again changes the old rating instead of adding one`() {
		rate(agata, """{"rating": 2}""")
		rate(agata, """{"rating": 3}""").andExpect {
			jsonPath("$.average") { value(3.0) }
			jsonPath("$.count") { value(1) }
			jsonPath("$.myRating") { value(3) }
		}
		assertEquals(1, reviewRepository.count())
	}

	@Test
	fun `a removed rating no longer counts, and removing twice is fine`() {
		rate(agata, """{"rating": 2}""")
		rate(marek, """{"rating": 4}""")

		repeat(2) {
			mockMvc.delete("/books/$solaris/review") { bearer(agata) }.andExpect {
				status { isOk() }
				jsonPath("$.average") { value(4.0) }
				jsonPath("$.count") { value(1) }
				jsonPath("$.myRating") { doesNotExist() }
			}
		}
	}

	@Test
	fun `a hidden review doesn't count`() {
		rate(agata, """{"rating": 1}""")
		rate(marek, """{"rating": 5}""")
		reviewRepository.findAll().filter { it.rating == 2.toShort() }.forEach {
			it.isHidden = true
			reviewRepository.save(it)
		}

		mockMvc.get("/books/$solaris/reviews") { bearer(agata) }.andExpect {
			jsonPath("$.average") { value(5.0) }
			jsonPath("$.count") { value(1) }
			jsonPath("$.myRating") { doesNotExist() }
		}
	}

	@Test
	fun `stars outside 1-5 or a too long text return 400`() {
		listOf(
			"""{"rating": 0}""",
			"""{"rating": 6}""",
			"""{"rating": 3, "content": "${"a".repeat(2001)}"}""",
		).forEach { body ->
			rate(agata, body).andExpect { status { isBadRequest() } }
		}
	}

	@Test
	fun `a book that doesn't exist returns 404`() {
		val missing = UUID.randomUUID()
		mockMvc.get("/books/$missing/reviews").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("BOOK_NOT_FOUND") }
		}
		mockMvc.put("/books/$missing/review") {
			bearer(agata)
			json("""{"rating": 5}""")
		}.andExpect { status { isNotFound() } }
	}

	private fun rate(token: String, body: String) =
		mockMvc.put("/books/$solaris/review") {
			bearer(token)
			json(body)
		}

	private fun registerAndLogIn(name: String, username: String): String {
		val email = "$username@example.com"
		mockMvc.post("/auth/register") {
			json("""{"name": "$name", "username": "$username", "email": "$email", "password": "secret-password"}""")
		}.andExpect { status { isCreated() } }
		val body = mockMvc.post("/auth/login") {
			json("""{"email": "$email", "password": "secret-password"}""")
		}.andReturn().response.contentAsString
		return Regex(""""token":"([^"]+)"""").find(body)!!.groupValues[1]
	}

	private fun MockHttpServletRequestDsl.json(body: String) {
		contentType = MediaType.APPLICATION_JSON
		content = body
	}

	private fun MockHttpServletRequestDsl.bearer(token: String) {
		header("Authorization", "Bearer $token")
	}
}
