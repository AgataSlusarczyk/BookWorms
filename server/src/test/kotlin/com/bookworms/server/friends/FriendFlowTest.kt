package com.bookworms.server.friends

import com.bookworms.server.TestcontainersConfiguration
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

// Friends: search, a request by id, accept or reject, the list, a friend's books.
// Agata looks for people; Marek and Maja (username "ma_ja") are there to be found.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class FriendFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var maja: String
	private lateinit var marekId: String
	private lateinit var majaId: String
	private lateinit var agataId: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		maja = registerAndLogIn("Maja", "ma_ja@example.com")
		agataId = id(agata)
		marekId = id(marek)
		majaId = id(maja)
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf(
			"copy_group_visibility", "book_copies", "books",
			"friendships", "group_invitations", "group_messages", "group_members", "groups", "users",
		)) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `search finds people by username, without their email and without me`() {
		mockMvc.get("/friends/search?query=MA") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(2) }
			// By username: ma_ja before marek.
			jsonPath("$[0].username") { value("ma_ja") }
			jsonPath("$[1].username") { value("marek") }
			jsonPath("$[0].email") { doesNotExist() }
		}
		// "ag" is only Agata herself.
		mockMvc.get("/friends/search?query=ag") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		// Under 2 letters: nothing.
		mockMvc.get("/friends/search?query=m") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `percent and underscore in a search are just letters`() {
		// "a_" without the escaping would mean "a and any letter" – marek and agata too.
		mockMvc.get("/friends/search?query=a_") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].username") { value("ma_ja") }
		}
		// "%%" without the escaping would be "anything" – everyone.
		mockMvc.get("/friends/search") { bearer(agata); param("query", "%%") }
			.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `an accepted request makes friends on both sides`() {
		sendRequest(agata, marekId).andExpect { status { isCreated() } }

		val requestId = field(
			mockMvc.get("/friends/requests") { bearer(marek) }.andExpect {
				jsonPath("$.length()") { value(1) }
				jsonPath("$[0].requesterName") { value("Agata") }
				jsonPath("$[0].requesterUsername") { value("agata") }
				jsonPath("$[0].requesterEmail") { doesNotExist() }
			},
			"id",
		)
		// Agata sent it, so she has nothing to answer.
		mockMvc.get("/friends/requests") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		// Someone waiting for an answer isn't in search anymore.
		mockMvc.get("/friends/search?query=marek") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }

		mockMvc.post("/friends/requests/$requestId/accept") { bearer(marek) }.andExpect { status { isNoContent() } }

		mockMvc.get("/friends") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].id") { value(marekId) }
			jsonPath("$[0].email") { doesNotExist() }
		}
		mockMvc.get("/friends") { bearer(marek) }.andExpect { jsonPath("$[0].id") { value(agataId) } }
		mockMvc.get("/friends/$marekId/books") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(0) }
		}
	}

	@Test
	fun `requests to myself, to someone unknown, or twice are refused`() {
		sendRequest(agata, agataId).andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("CANNOT_FRIEND_SELF") }
		}
		sendRequest(agata, "00000000-0000-0000-0000-000000000000").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("USER_NOT_FOUND") }
		}
		sendRequest(agata, marekId).andExpect { status { isCreated() } }
		sendRequest(agata, marekId).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("FRIEND_REQUEST_EXISTS") }
		}
		// The other way round is the same pair.
		sendRequest(marek, agataId).andExpect { status { isConflict() } }
	}

	@Test
	fun `only the one asked can answer, and a rejected request can be sent again`() {
		sendRequest(agata, marekId)
		val requestId = field(mockMvc.get("/friends/requests") { bearer(marek) }, "id")

		mockMvc.post("/friends/requests/$requestId/accept") { bearer(agata) }.andExpect { status { isNotFound() } }
		mockMvc.post("/friends/requests/$requestId/accept") { bearer(maja) }.andExpect { status { isNotFound() } }

		mockMvc.post("/friends/requests/$requestId/reject") { bearer(marek) }.andExpect { status { isNoContent() } }
		mockMvc.get("/friends") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		sendRequest(agata, marekId).andExpect { status { isCreated() } }
	}

	@Test
	fun `a removed friend's books can't be seen anymore`() {
		mockMvc.get("/friends/$marekId/books") { bearer(agata) }.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("FRIEND_NOT_FOUND") }
		}
		sendRequest(agata, marekId)
		val requestId = field(mockMvc.get("/friends/requests") { bearer(marek) }, "id")
		mockMvc.post("/friends/requests/$requestId/accept") { bearer(marek) }

		mockMvc.delete("/friends/$agataId") { bearer(marek) }.andExpect { status { isNoContent() } }

		mockMvc.get("/friends") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/friends/$marekId/books") { bearer(agata) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `a friend sees only the copies shown in a group, without the owner's own description`() {
		makeFriends(agata, marek, marekId)
		// A new copy is shown in all the owner's groups – Marek is in one.
		mockMvc.post("/groups") { bearer(marek); json("""{"name": "Czytelnia"}""") }.andExpect { status { isCreated() } }
		val shown = addCopy(marek, """{"title": "Solaris", "authors": "Lem", "description": "My own words"}""")
		val hidden = addCopy(marek, """{"title": "Hidden", "authors": "Nobody"}""")
		val lost = addCopy(marek, """{"title": "Lost", "authors": "Nobody"}""")
		mockMvc.put("/copies/$hidden/groups") { bearer(marek); json("""{"groupIds": []}""") }
			.andExpect { status { isOk() } }
		mockMvc.patch("/copies/$lost") { bearer(marek); json("""{"availability": "LOST"}""") }
			.andExpect { status { isOk() } }

		mockMvc.get("/friends/$marekId/books") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].copyId") { value(shown) }
			jsonPath("$[0].title") { value("Solaris") }
			jsonPath("$[0].ownerNote") { doesNotExist() }
			jsonPath("$[0].bookDescription") { doesNotExist() }
		}
	}

	private fun makeFriends(asker: String, answerer: String, answererId: String) {
		sendRequest(asker, answererId).andExpect { status { isCreated() } }
		val requestId = field(mockMvc.get("/friends/requests") { bearer(answerer) }, "id")
		mockMvc.post("/friends/requests/$requestId/accept") { bearer(answerer) }.andExpect { status { isNoContent() } }
	}

	private fun addCopy(token: String, body: String): String =
		field(mockMvc.post("/me/copies") { bearer(token); json(body) }.andExpect { status { isCreated() } }, "id")

	private fun sendRequest(token: String, userId: String) =
		mockMvc.post("/friends/requests") {
			bearer(token)
			json("""{"userId": "$userId"}""")
		}

	private fun id(token: String): String = field(mockMvc.get("/me") { bearer(token) }, "id")

	private fun registerAndLogIn(name: String, email: String): String {
		val username = email.substringBefore('@')
		mockMvc.post("/auth/register") {
			json("""{"name": "$name", "username": "$username", "email": "$email", "password": "secret-password"}""")
		}.andExpect { status { isCreated() } }
		val body = mockMvc.post("/auth/login") {
			json("""{"email": "$email", "password": "secret-password"}""")
		}.andReturn().response.contentAsString
		return Regex(""""token":"([^"]+)"""").find(body)!!.groupValues[1]
	}

	// The first "name": "value" in the answer.
	private fun field(result: ResultActionsDsl, name: String): String =
		Regex(""""$name":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1]

	private fun MockHttpServletRequestDsl.json(body: String) {
		contentType = MediaType.APPLICATION_JSON
		content = body
	}

	private fun MockHttpServletRequestDsl.bearer(token: String) {
		header("Authorization", "Bearer $token")
	}
}
