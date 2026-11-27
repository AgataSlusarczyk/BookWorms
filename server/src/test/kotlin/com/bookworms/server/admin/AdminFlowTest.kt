package com.bookworms.server.admin

import com.bookworms.server.TestcontainersConfiguration
import com.bookworms.server.users.GlobalRole
import com.bookworms.server.users.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
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
class AdminFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val userRepository: UserRepository,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var admin: String
	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var adminId: UUID
	private lateinit var agataId: UUID
	private lateinit var groupId: UUID
	// Agata's copy of Solaris, shown in her group with Marek.
	private lateinit var solaris: UUID
	private lateinit var solarisCopy: UUID

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		admin = registerAndLogIn("Admin", "boss")
		agata = registerAndLogIn("Agata", "agata")
		marek = registerAndLogIn("Marek", "marek")
		adminId = userRepository.findByEmail("boss@example.com")!!.id!!
		agataId = userRepository.findByEmail("agata@example.com")!!.id!!
		// Made admin in the database, the only way there is.
		userRepository.save(userRepository.findById(adminId).get().apply { globalRole = GlobalRole.SYSTEM_ADMIN })

		val group = mockMvc.post("/groups") {
			bearer(agata)
			json("""{"name": "Czytelnia"}""")
		}.andExpect { status { isCreated() } }.andReturn().response.contentAsString
		groupId = UUID.fromString(field(group, "id"))
		mockMvc.post("/groups/join") {
			bearer(marek)
			json("""{"code": "${field(group, "inviteCode")}"}""")
		}.andExpect { status { isOk() } }
		val copy = mockMvc.post("/me/copies") {
			bearer(agata)
			json("""{"isbn": "9788308049461", "title": "Solaris", "authors": "Stanisław Lem"}""")
		}.andExpect { status { isCreated() } }.andReturn().response.contentAsString
		solarisCopy = UUID.fromString(field(copy, "id"))
		solaris = UUID.fromString(Regex(""""book":\{"id":"([^"]+)"""").find(copy)!!.groupValues[1])
	}

	@AfterEach
	fun cleanDatabase() {
		for (table in listOf(
			"admin_logs", "notifications", "availability_watchers", "borrowings", "borrow_requests", "copy_group_visibility",
			"book_copies", "group_messages", "group_invitations", "group_members", "groups", "book_list_items", "book_lists",
			"user_book_status", "book_reviews", "books", "users",
		)) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `only a system admin gets in`() {
		mockMvc.get("/admin/stats").andExpect { status { isUnauthorized() } }
		for (path in listOf("/admin/stats", "/admin/users", "/admin/groups", "/admin/logs")) {
			mockMvc.get(path) { bearer(agata) }.andExpect {
				status { isForbidden() }
				jsonPath("$.code") { value("NOT_SYSTEM_ADMIN") }
			}
		}
		mockMvc.delete("/admin/books/$solaris") { bearer(agata) }.andExpect { status { isForbidden() } }
		mockMvc.delete("/admin/groups/$groupId") { bearer(marek) }.andExpect { status { isForbidden() } }
	}

	@Test
	fun `the app learns who is an admin from the user it gets`() {
		mockMvc.get("/me") { bearer(admin) }.andExpect { jsonPath("$.role") { value("SYSTEM_ADMIN") } }
		mockMvc.get("/me") { bearer(agata) }.andExpect { jsonPath("$.role") { value("USER") } }
		mockMvc.post("/auth/login") { json("""{"email": "boss@example.com", "password": "secret-password"}""") }
			.andExpect { jsonPath("$.user.role") { value("SYSTEM_ADMIN") } }
	}

	@Test
	fun `the panel counts everything and lists the accounts`() {
		mockMvc.get("/admin/stats") { bearer(admin) }.andExpect {
			status { isOk() }
			jsonPath("$.users") { value(3) }
			jsonPath("$.blockedUsers") { value(0) }
			jsonPath("$.books") { value(1) }
			jsonPath("$.copies") { value(1) }
			jsonPath("$.groups") { value(1) }
		}
		mockMvc.get("/admin/users") { bearer(admin) }.andExpect {
			jsonPath("$.length()") { value(3) }
			// The newest first.
			jsonPath("$[0].username") { value("marek") }
			jsonPath("$[0].email") { value("marek@example.com") }
			jsonPath("$[0].status") { value("ACTIVE") }
		}
		// By name, username or email, in any case.
		mockMvc.get("/admin/users?query=AGA") { bearer(admin) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].name") { value("Agata") }
		}
	}

	@Test
	fun `a blocked user can't log in, and their token stops working`() {
		mockMvc.post("/admin/users/$agataId/block") { bearer(admin) }.andExpect {
			status { isOk() }
			jsonPath("$.status") { value("BLOCKED") }
		}
		mockMvc.get("/me") { bearer(agata) }.andExpect {
			status { isUnauthorized() }
			jsonPath("$.code") { value("ACCOUNT_BLOCKED") }
		}
		mockMvc.post("/auth/login") { json("""{"email": "agata@example.com", "password": "secret-password"}""") }
			.andExpect {
				status { isForbidden() }
				jsonPath("$.code") { value("ACCOUNT_BLOCKED") }
			}
		// A wrong password doesn't tell anyone the account is blocked.
		mockMvc.post("/auth/login") { json("""{"email": "agata@example.com", "password": "wrong-password"}""") }
			.andExpect { status { isUnauthorized() } }
		mockMvc.get("/admin/stats") { bearer(admin) }.andExpect { jsonPath("$.blockedUsers") { value(1) } }

		mockMvc.post("/admin/users/$agataId/unblock") { bearer(admin) }.andExpect {
			jsonPath("$.status") { value("ACTIVE") }
		}
		mockMvc.get("/me") { bearer(agata) }.andExpect { status { isOk() } }
	}

	@Test
	fun `an admin can't block themselves or another admin`() {
		mockMvc.post("/admin/users/$adminId/block") { bearer(admin) }.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("CANNOT_BLOCK_SELF") }
		}
		userRepository.save(userRepository.findById(agataId).get().apply { globalRole = GlobalRole.SYSTEM_ADMIN })
		mockMvc.post("/admin/users/$agataId/block") { bearer(admin) }.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("CANNOT_BLOCK_ADMIN") }
		}
	}

	@Test
	fun `an admin edits any book for everyone`() {
		mockMvc.put("/admin/books/$solaris") {
			bearer(admin)
			json("""{"title": " Solaris (wyd. II) ", "authors": "Stanisław Lem", "publishedYear": 1961, "description": "Ocean."}""")
		}.andExpect {
			status { isOk() }
			jsonPath("$.title") { value("Solaris (wyd. II)") }
			jsonPath("$.isbn") { value("9788308049461") }
		}
		mockMvc.get("/books/isbn/9788308049461").andExpect {
			jsonPath("$.title") { value("Solaris (wyd. II)") }
			jsonPath("$.description") { value("Ocean.") }
		}
		mockMvc.put("/admin/books/$solaris") {
			bearer(admin)
			json("""{"title": "", "authors": "Lem"}""")
		}.andExpect { status { isBadRequest() } }
	}

	@Test
	fun `deleting a book takes everything about it along`() {
		// Marek asks for Agata's copy, rates the book, wants to read it and puts it on a list.
		mockMvc.post("/copies/$solarisCopy/requests") { bearer(marek) }.andExpect { status { isCreated() } }
		mockMvc.put("/books/$solaris/review") {
			bearer(marek)
			json("""{"rating": 4, "content": "Great."}""")
		}.andExpect { status { isOk() } }
		mockMvc.put("/me/shelf/$solaris/status") {
			bearer(marek)
			json("""{"status": "TO_READ"}""")
		}.andExpect { status { is2xxSuccessful() } }
		val list = mockMvc.post("/me/lists") {
			bearer(marek)
			json("""{"name": "Sci-fi"}""")
		}.andExpect { status { isCreated() } }.andReturn().response.contentAsString
		val listId = field(list, "id")
		mockMvc.put("/me/lists/$listId/books/$solaris") { bearer(marek) }.andExpect { status { is2xxSuccessful() } }

		mockMvc.delete("/admin/books/$solaris") { bearer(admin) }.andExpect { status { isNoContent() } }

		mockMvc.get("/books/isbn/9788308049461").andExpect { status { isNotFound() } }
		mockMvc.get("/me/copies") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/requests/sent") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/requests/received") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/lists/$listId") { bearer(marek) }.andExpect {
			status { isOk() }
			jsonPath("$.books.length()") { value(0) }
		}
		for (table in listOf("book_reviews", "user_book_status", "borrow_requests", "notifications")) {
			assertEquals(0, jdbc.queryForObject("select count(*) from $table", Int::class.java), table)
		}
		mockMvc.get("/admin/logs") { bearer(admin) }.andExpect {
			jsonPath("$[0].action") { value("DELETE_BOOK") }
			jsonPath("$[0].targetType") { value("BOOK") }
			jsonPath("$[0].description") { value("Solaris") }
			jsonPath("$[0].admin.name") { value("Admin") }
		}

		mockMvc.delete("/admin/books/$solaris") { bearer(admin) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `a hidden review disappears for everyone`() {
		val reviews = mockMvc.put("/books/$solaris/review") {
			bearer(marek)
			json("""{"rating": 1, "content": "Spam spam spam"}""")
		}.andReturn().response.contentAsString
		val reviewId = Regex(""""reviews":\[\{"id":"([^"]+)"""").find(reviews)!!.groupValues[1]

		mockMvc.post("/admin/reviews/$reviewId/hide") { bearer(agata) }.andExpect { status { isForbidden() } }
		mockMvc.post("/admin/reviews/$reviewId/hide") { bearer(admin) }.andExpect { status { isNoContent() } }

		mockMvc.get("/books/$solaris/reviews").andExpect {
			jsonPath("$.count") { value(0) }
			jsonPath("$.reviews.length()") { value(0) }
		}
		mockMvc.get("/admin/logs") { bearer(admin) }.andExpect {
			jsonPath("$[0].action") { value("HIDE_REVIEW") }
			jsonPath("$[0].description") { value("@marek · Solaris") }
		}
	}

	@Test
	fun `an admin sees and deletes a group they aren't in`() {
		mockMvc.get("/admin/groups") { bearer(admin) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].name") { value("Czytelnia") }
			jsonPath("$[0].memberCount") { value(2) }
			jsonPath("$[0].createdBy.name") { value("Agata") }
		}
		mockMvc.delete("/admin/groups/$groupId") { bearer(admin) }.andExpect { status { isNoContent() } }

		mockMvc.get("/me/groups") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/admin/groups") { bearer(admin) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.delete("/admin/groups/$groupId") { bearer(admin) }.andExpect { status { isNotFound() } }
	}

	private fun field(json: String, name: String) = Regex(""""$name":"([^"]+)"""").find(json)!!.groupValues[1]

	private fun registerAndLogIn(name: String, username: String): String {
		val email = "$username@example.com"
		mockMvc.post("/auth/register") {
			json("""{"name": "$name", "username": "$username", "email": "$email", "password": "secret-password"}""")
		}.andExpect { status { isCreated() } }
		val body = mockMvc.post("/auth/login") {
			json("""{"email": "$email", "password": "secret-password"}""")
		}.andReturn().response.contentAsString
		return field(body, "token")
	}

	private fun MockHttpServletRequestDsl.json(body: String) {
		contentType = MediaType.APPLICATION_JSON
		content = body
	}

	private fun MockHttpServletRequestDsl.bearer(token: String) {
		header("Authorization", "Bearer $token")
	}
}
