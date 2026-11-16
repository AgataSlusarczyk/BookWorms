package com.bookworms.server.shelves

import com.bookworms.server.TestcontainersConfiguration
import com.bookworms.server.books.Book
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.BookSource
import com.bookworms.server.lists.BookListItemRepository
import com.bookworms.server.lists.BookListRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ShelfFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val statusRepository: UserBookStatusRepository,
	@Autowired private val listRepository: BookListRepository,
	@Autowired private val itemRepository: BookListItemRepository,
	@Autowired private val bookRepository: BookRepository,
	@Autowired private val userRepository: UserRepository,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var solaris: UUID
	private lateinit var dune: UUID

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		solaris = bookRepository.save(Book(BookSource.MANUAL, "Solaris", "Stanisław Lem", "9788308049461")).id!!
		dune = bookRepository.save(Book(BookSource.MANUAL, "Dune", "Frank Herbert", "9788381884850")).id!!
		agata = registerAndLogIn("Agata", "agata")
		marek = registerAndLogIn("Marek", "marek")
	}

	// The other test classes share this database, and shelves keep their books and users from being deleted.
	@AfterEach
	fun cleanDatabase() {
		itemRepository.deleteAllInBatch()
		listRepository.deleteAllInBatch()
		statusRepository.deleteAllInBatch()
		bookRepository.deleteAllInBatch()
		userRepository.deleteAllInBatch()
	}

	@Test
	fun `the shelves need logging in`() {
		mockMvc.get("/me/shelf").andExpect { status { isUnauthorized() } }
		mockMvc.put("/me/shelf/$solaris/favorite").andExpect { status { isUnauthorized() } }
		mockMvc.put("/me/shelf/$solaris/status") { json("""{"status": "READ"}""") }
			.andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `a book starts on no shelf`() {
		mockMvc.get("/me/shelf/$solaris") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.status") { doesNotExist() }
			jsonPath("$.favorite") { value(false) }
		}
		mockMvc.get("/me/shelf") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `the reading status puts the book on a shelf, and taking it back takes it off`() {
		setStatus(agata, solaris, "TO_READ").andExpect {
			status { isOk() }
			jsonPath("$.status") { value("TO_READ") }
			jsonPath("$.favorite") { value(false) }
		}
		setStatus(agata, solaris, "READ").andExpect { jsonPath("$.status") { value("READ") } }
		mockMvc.get("/me/shelf") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].book.title") { value("Solaris") }
			jsonPath("$[0].status") { value("READ") }
			jsonPath("$[0].statusChangedAt") { exists() }
			jsonPath("$[0].favorite") { value(false) }
		}

		mockMvc.delete("/me/shelf/$solaris/status") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.status") { doesNotExist() }
		}
		mockMvc.get("/me/shelf") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }

		// It can be picked again after that.
		setStatus(agata, solaris, "READING").andExpect { jsonPath("$.status") { value("READING") } }
		mockMvc.get("/me/shelf/$solaris") { bearer(agata) }.andExpect { jsonPath("$.status") { value("READING") } }
	}

	@Test
	fun `the heart is separate from the reading status`() {
		favorite(agata, solaris).andExpect {
			status { isOk() }
			jsonPath("$.favorite") { value(true) }
		}
		// Twice is fine.
		favorite(agata, solaris).andExpect { status { isOk() } }
		setStatus(agata, solaris, "READ")
		setStatus(agata, dune, "TO_READ")

		mockMvc.get("/me/shelf/$solaris") { bearer(agata) }.andExpect {
			jsonPath("$.status") { value("READ") }
			jsonPath("$.favorite") { value(true) }
		}
		mockMvc.get("/me/shelf") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(2) } }

		mockMvc.delete("/me/shelf/$solaris/favorite") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.favorite") { value(false) }
			jsonPath("$.status") { value("READ") }
		}
		// Solaris is still on Read.
		mockMvc.get("/me/shelf") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(2) } }
	}

	@Test
	fun `each person has their own shelves`() {
		favorite(agata, solaris)
		setStatus(agata, dune, "READ")

		mockMvc.get("/me/shelf") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/shelf/$solaris") { bearer(marek) }.andExpect { jsonPath("$.favorite") { value(false) } }
	}

	@Test
	fun `an unknown book or status is refused`() {
		val nothing = UUID.randomUUID()
		favorite(agata, nothing).andExpect { status { isNotFound() } }
		setStatus(agata, nothing, "READ").andExpect { status { isNotFound() } }
		setStatus(agata, solaris, "SOMETHING").andExpect { status { isBadRequest() } }
		mockMvc.put("/me/shelf/$solaris/status") {
			bearer(agata)
			json("{}")
		}.andExpect { status { isBadRequest() } }
	}

	private fun setStatus(token: String, bookId: UUID, status: String) =
		mockMvc.put("/me/shelf/$bookId/status") {
			bearer(token)
			json("""{"status": "$status"}""")
		}

	private fun favorite(token: String, bookId: UUID) =
		mockMvc.put("/me/shelf/$bookId/favorite") { bearer(token) }

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
