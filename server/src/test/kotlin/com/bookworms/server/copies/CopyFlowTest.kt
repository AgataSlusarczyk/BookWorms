package com.bookworms.server.copies

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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class CopyFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val copyRepository: BookCopyRepository,
	@Autowired private val bookRepository: BookRepository,
	@Autowired private val userRepository: UserRepository,
) {

	private lateinit var agata: String
	private lateinit var marek: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		bookRepository.save(Book(BookSource.MANUAL, "Solaris", "Stanisław Lem", "9788308049461"))
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
	}

	// The other test classes share this database, and a copy keeps its book and owner from being deleted.
	@AfterEach
	fun cleanDatabase() {
		copyRepository.deleteAllInBatch()
		bookRepository.deleteAllInBatch()
		userRepository.deleteAllInBatch()
	}

	@Test
	fun `my books need logging in`() {
		mockMvc.get("/me/copies").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `a book from the catalog gets the copy, and shows up in my books`() {
		addCopy(agata, """{"isbn": "978-83-08-04946-1", "title": "Solaris (typed)", "authors": "Lem"}""")
			.andExpect {
				status { isCreated() }
				// The catalog's title, not the typed one – the copy belongs to the book that was already there.
				jsonPath("$.book.title") { value("Solaris") }
				jsonPath("$.availability") { value("AVAILABLE") }
				jsonPath("$.canEditBook") { value(false) }
			}

		mockMvc.get("/me/copies") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].book.isbn") { value("9788308049461") }
		}
		assertEquals(1, bookRepository.count())
	}

	@Test
	fun `a book that isn't in the catalog is added to it, and an old ISBN-10 becomes ISBN-13`() {
		addCopy(agata, """{"isbn": "0-306-40615-2", "title": "My Book", "authors": "Me", "publishedYear": 2001}""")
			.andExpect {
				status { isCreated() }
				jsonPath("$.book.isbn") { value("9780306406157") }
				jsonPath("$.book.publishedYear") { value(2001) }
				jsonPath("$.canEditBook") { value(true) }
			}
		assertEquals(2, bookRepository.count())
	}

	@Test
	fun `two people with the same book share one catalog book`() {
		addCopy(agata, """{"isbn": "9780306406157", "title": "My Book", "authors": "Me"}""")
		addCopy(marek, """{"isbn": "9780306406157", "title": "My Book", "authors": "Me"}""")

		assertEquals(2, copyRepository.count())
		assertEquals(2, bookRepository.count())
	}

	@Test
	fun `title and author are required, and the ISBN must have 10 or 13 digits`() {
		addCopy(agata, """{"title": " ", "authors": "Lem"}""").andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("VALIDATION_FAILED") }
		}
		addCopy(agata, """{"isbn": "12345", "title": "Solaris", "authors": "Lem"}""").andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("INVALID_ISBN") }
		}
	}

	@Test
	fun `the owner can change availability`() {
		val id = addCopyAndGetId(agata, """{"isbn": "9788308049461", "title": "Solaris", "authors": "Lem"}""")

		mockMvc.patch("/copies/$id") {
			bearer(agata)
			contentType = MediaType.APPLICATION_JSON
			content = """{"availability": "LOST"}"""
		}.andExpect {
			status { isOk() }
			jsonPath("$.availability") { value("LOST") }
		}
	}

	@Test
	fun `someone else's copy looks like it doesn't exist`() {
		val id = addCopyAndGetId(agata, """{"isbn": "9788308049461", "title": "Solaris", "authors": "Lem"}""")

		mockMvc.get("/me/copies") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.patch("/copies/$id") {
			bearer(marek)
			contentType = MediaType.APPLICATION_JSON
			content = """{"availability": "LOST"}"""
		}.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("COPY_NOT_FOUND") }
		}
		mockMvc.delete("/copies/$id") { bearer(marek) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `only a book typed in by hand can be edited`() {
		val fromCatalog = addCopyAndGetId(agata, """{"isbn": "9788308049461", "title": "Solaris", "authors": "Lem"}""")
		val typedIn = addCopyAndGetId(agata, """{"title": "Notes", "authors": "Me"}""")

		editBook(agata, fromCatalog, """{"title": "Changed", "authors": "Lem"}""").andExpect {
			status { isForbidden() }
			jsonPath("$.code") { value("BOOK_NOT_EDITABLE") }
		}
		editBook(agata, typedIn, """{"title": "My notes", "authors": "Agata", "publisher": "Home"}""").andExpect {
			status { isOk() }
			jsonPath("$.book.title") { value("My notes") }
			jsonPath("$.book.publisher") { value("Home") }
		}
	}

	@Test
	fun `a removed copy is gone from my books`() {
		val id = addCopyAndGetId(agata, """{"isbn": "9788308049461", "title": "Solaris", "authors": "Lem"}""")

		mockMvc.delete("/copies/$id") { bearer(agata) }.andExpect { status { isNoContent() } }

		mockMvc.get("/me/copies") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.delete("/copies/$id") { bearer(agata) }.andExpect { status { isNotFound() } }
		// Still in the database, so phones can learn it was removed.
		assertEquals(1, copyRepository.count())
	}

	private fun addCopy(token: String, json: String) =
		mockMvc.post("/me/copies") {
			bearer(token)
			contentType = MediaType.APPLICATION_JSON
			content = json
		}

	private fun addCopyAndGetId(token: String, json: String): String {
		val body = addCopy(token, json).andExpect { status { isCreated() } }.andReturn().response.contentAsString
		return Regex(""""id":"([^"]+)"""").find(body)!!.groupValues[1]
	}

	private fun editBook(token: String, copyId: String, json: String) =
		mockMvc.put("/copies/$copyId/book") {
			bearer(token)
			contentType = MediaType.APPLICATION_JSON
			content = json
		}

	private fun registerAndLogIn(name: String, email: String): String {
		mockMvc.post("/auth/register") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name": "$name", "email": "$email", "password": "secret-password"}"""
		}.andExpect { status { isCreated() } }
		val body = mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email": "$email", "password": "secret-password"}"""
		}.andReturn().response.contentAsString
		return Regex(""""token":"([^"]+)"""").find(body)!!.groupValues[1]
	}

	private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer(token: String) {
		header("Authorization", "Bearer $token")
	}
}
