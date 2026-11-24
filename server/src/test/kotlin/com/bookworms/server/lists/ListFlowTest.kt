package com.bookworms.server.lists

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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ListFlowTest(
	@Autowired private val mockMvc: MockMvc,
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

	// The other test classes share this database, and lists keep their books and users from being deleted.
	@AfterEach
	fun cleanDatabase() {
		itemRepository.deleteAllInBatch()
		listRepository.deleteAllInBatch()
		bookRepository.deleteAllInBatch()
		userRepository.deleteAllInBatch()
	}

	@Test
	fun `lists need logging in`() {
		mockMvc.get("/me/lists").andExpect { status { isUnauthorized() } }
		mockMvc.post("/me/lists") { json("""{"name": "Sci-fi"}""") }.andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `a new list shows up on My lists, newest first`() {
		mockMvc.get("/me/lists") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(0) }
		}

		createList(agata, """{"name": "  Sci-fi  ", "description": "Space and robots"}""").andExpect {
			status { isCreated() }
			jsonPath("$.id") { exists() }
			// Without the spaces around it.
			jsonPath("$.name") { value("Sci-fi") }
			jsonPath("$.description") { value("Space and robots") }
			jsonPath("$.bookCount") { value(0) }
		}
		createList(agata, """{"name": "Holidays", "description": "   "}""").andExpect {
			status { isCreated() }
			// Only spaces = no description.
			jsonPath("$.description") { doesNotExist() }
		}

		mockMvc.get("/me/lists") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(2) }
			jsonPath("$[0].name") { value("Holidays") }
			jsonPath("$[1].name") { value("Sci-fi") }
		}
	}

	@Test
	fun `a list needs a name of at most 30 letters`() {
		createList(agata, """{"name": "   "}""").andExpect { status { isBadRequest() } }
		createList(agata, """{"name": "${"a".repeat(31)}"}""").andExpect { status { isBadRequest() } }
		createList(agata, """{"name": "${"a".repeat(30)}"}""").andExpect { status { isCreated() } }
	}

	@Test
	fun `two lists of one person can't have the same name`() {
		createList(agata, """{"name": "Sci-fi"}""").andExpect { status { isCreated() } }
		createList(agata, """{"name": "SCI-FI "}""").andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("LIST_NAME_TAKEN") }
		}
		// Someone else can.
		createList(marek, """{"name": "Sci-fi"}""").andExpect { status { isCreated() } }
	}

	@Test
	fun `Favorites isn't one of the custom lists`() {
		mockMvc.put("/me/shelf/$solaris/favorite") { bearer(agata) }.andExpect { status { isOk() } }

		mockMvc.get("/me/lists") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		// Its name is taken, so two lists don't look the same.
		createList(agata, """{"name": "favorites"}""").andExpect { status { isConflict() } }
	}

	@Test
	fun `each person sees only their own lists`() {
		createList(agata, """{"name": "Sci-fi"}""")

		mockMvc.get("/me/lists") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `an opened list shows its books, the newest first`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi", "description": "Space"}"""))
		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Sci-fi") }
			jsonPath("$.description") { value("Space") }
			jsonPath("$.books.length()") { value(0) }
		}

		addBook(agata, sciFi, solaris).andExpect { status { isNoContent() } }
		addBook(agata, sciFi, dune).andExpect { status { isNoContent() } }

		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect {
			jsonPath("$.books.length()") { value(2) }
			jsonPath("$.books[0].book.title") { value("Dune") }
			jsonPath("$.books[0].addedAt") { exists() }
			jsonPath("$.books[1].book.title") { value("Solaris") }
		}
		mockMvc.get("/me/lists") { bearer(agata) }.andExpect { jsonPath("$[0].bookCount") { value(2) } }
	}

	@Test
	fun `nobody else can open, change or delete someone's list`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi"}"""))

		mockMvc.get("/me/lists/$sciFi") { bearer(marek) }.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("LIST_NOT_FOUND") }
		}
		updateList(marek, sciFi, """{"name": "Mine now"}""").andExpect { status { isNotFound() } }
		mockMvc.delete("/me/lists/$sciFi") { bearer(marek) }.andExpect { status { isNotFound() } }
		mockMvc.get("/me/lists/${UUID.randomUUID()}") { bearer(agata) }.andExpect { status { isNotFound() } }

		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { jsonPath("$.name") { value("Sci-fi") } }
	}

	@Test
	fun `Favorites can't be opened, changed or deleted as a custom list`() {
		mockMvc.put("/me/shelf/$solaris/favorite") { bearer(agata) }
		val agataId = userRepository.findByEmail("agata@example.com")!!.id!!
		val favorites = listRepository.findByOwnerIdAndType(agataId, ListType.FAVORITES)!!.id!!

		mockMvc.get("/me/lists/$favorites") { bearer(agata) }.andExpect { status { isNotFound() } }
		updateList(agata, favorites, """{"name": "Loved"}""").andExpect { status { isNotFound() } }
		mockMvc.delete("/me/lists/$favorites") { bearer(agata) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `Edit list changes the name and the description`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi", "description": "Space"}"""))
		createList(agata, """{"name": "Holidays"}""")

		updateList(agata, sciFi, """{"name": "Science fiction ", "description": ""}""").andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Science fiction") }
			jsonPath("$.description") { doesNotExist() }
		}
		// Its own name with other letters is fine; another list's name isn't.
		updateList(agata, sciFi, """{"name": "SCIENCE FICTION"}""").andExpect { status { isOk() } }
		updateList(agata, sciFi, """{"name": "holidays"}""").andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("LIST_NAME_TAKEN") }
		}
		updateList(agata, sciFi, """{"name": ""}""").andExpect { status { isBadRequest() } }

		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { jsonPath("$.name") { value("SCIENCE FICTION") } }
	}

	@Test
	fun `a deleted list is gone, and its name is free again`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi"}"""))
		addBook(agata, sciFi, solaris)

		mockMvc.delete("/me/lists/$sciFi") { bearer(agata) }.andExpect { status { isNoContent() } }

		mockMvc.get("/me/lists") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { status { isNotFound() } }
		mockMvc.delete("/me/lists/$sciFi") { bearer(agata) }.andExpect { status { isNotFound() } }
		createList(agata, """{"name": "Sci-fi"}""").andExpect { status { isCreated() } }
	}

	@Test
	fun `a book goes on a list once, and can be taken off again`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi"}"""))

		addBook(agata, sciFi, solaris).andExpect { status { isNoContent() } }
		// Twice is fine – it's still there once.
		addBook(agata, sciFi, solaris).andExpect { status { isNoContent() } }
		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { jsonPath("$.books.length()") { value(1) } }

		mockMvc.delete("/me/lists/$sciFi/books/$solaris") { bearer(agata) }.andExpect { status { isNoContent() } }
		// Taking off a book that isn't there is fine too.
		mockMvc.delete("/me/lists/$sciFi/books/$solaris") { bearer(agata) }.andExpect { status { isNoContent() } }
		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { jsonPath("$.books.length()") { value(0) } }
	}

	@Test
	fun `only real books go on your own lists`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi"}"""))

		addBook(agata, sciFi, UUID.randomUUID()).andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("BOOK_NOT_FOUND") }
		}
		addBook(marek, sciFi, solaris).andExpect { status { isNotFound() } }
		mockMvc.delete("/me/lists/$sciFi/books/$solaris") { bearer(marek) }.andExpect { status { isNotFound() } }
		mockMvc.get("/me/lists/$sciFi") { bearer(agata) }.andExpect { jsonPath("$.books.length()") { value(0) } }
	}

	@Test
	fun `asked about one book, each list says whether it has it`() {
		val sciFi = listId(createList(agata, """{"name": "Sci-fi"}"""))
		createList(agata, """{"name": "Holidays"}""")
		addBook(agata, sciFi, solaris)

		mockMvc.get("/me/lists?bookId=$solaris") { bearer(agata) }.andExpect {
			jsonPath("$[0].name") { value("Holidays") }
			jsonPath("$[0].containsBook") { value(false) }
			jsonPath("$[1].name") { value("Sci-fi") }
			jsonPath("$[1].containsBook") { value(true) }
		}
		// Without bookId it isn't there at all.
		mockMvc.get("/me/lists") { bearer(agata) }.andExpect { jsonPath("$[0].containsBook") { doesNotExist() } }
	}

	private fun addBook(token: String, listId: UUID, bookId: UUID) =
		mockMvc.put("/me/lists/$listId/books/$bookId") { bearer(token) }

	private fun updateList(token: String, listId: UUID, body: String) =
		mockMvc.put("/me/lists/$listId") {
			bearer(token)
			json(body)
		}

	private fun listId(result: ResultActionsDsl): UUID =
		UUID.fromString(Regex(""""id":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1])

	private fun createList(token: String, body: String) =
		mockMvc.post("/me/lists") {
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
