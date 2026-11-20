package com.bookworms.server.chat

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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertContains
import kotlin.test.assertFalse

// The group's chat: writing, reading page by page, and hearing new messages live.
// Agata makes "Czytelnia" and Marek joins it in every test. Zosia is never in it.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ChatFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var zosia: String
	private lateinit var group: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		zosia = registerAndLogIn("Zosia", "zosia@example.com")
		val created = mockMvc.post("/groups") { bearer(agata); json("""{"name": "Czytelnia"}""") }
		group = field(created, "id")
		val code = field(created, "inviteCode")
		mockMvc.post("/groups/join") { bearer(marek); json("""{"code": "$code"}""") }.andExpect { status { isOk() } }
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf("group_messages", "copy_group_visibility", "group_members", "groups", "users")) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `members write and read the chat, the newest first`() {
		send(agata, "  Who has Solaris?  ").andExpect {
			status { isCreated() }
			jsonPath("$.content") { value("Who has Solaris?") }
			jsonPath("$.sender.name") { value("Agata") }
			jsonPath("$.groupId") { value(group) }
		}
		send(marek, "Me!")

		mockMvc.get("/groups/$group/messages") { bearer(marek) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(2) }
			jsonPath("$[0].content") { value("Me!") }
			jsonPath("$[0].sender.name") { value("Marek") }
			jsonPath("$[1].content") { value("Who has Solaris?") }
		}
	}

	@Test
	fun `older messages come page by page, without gaps or repeats`() {
		val ids = (1..5).map { field(send(agata, "Message $it"), "id") }

		mockMvc.get("/groups/$group/messages?limit=2") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(2) }
			jsonPath("$[0].content") { value("Message 5") }
			jsonPath("$[1].content") { value("Message 4") }
		}
		mockMvc.get("/groups/$group/messages?limit=2&before=${ids[3]}") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(2) }
			jsonPath("$[0].content") { value("Message 3") }
			jsonPath("$[1].content") { value("Message 2") }
		}
		mockMvc.get("/groups/$group/messages?limit=2&before=${ids[1]}") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].content") { value("Message 1") }
		}
	}

	@Test
	fun `an empty or too long message isn't sent`() {
		send(agata, "   ").andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("VALIDATION_FAILED") }
		}
		send(agata, "a".repeat(2001)).andExpect { status { isBadRequest() } }
	}

	@Test
	fun `someone outside the group can't read, write or listen`() {
		send(agata, "Just for us")

		mockMvc.get("/groups/$group/messages") { bearer(zosia) }.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("GROUP_NOT_FOUND") }
		}
		send(zosia, "Let me in").andExpect { status { isNotFound() } }
		mockMvc.get("/groups/$group/messages/stream") {
			bearer(zosia)
			accept = MediaType.TEXT_EVENT_STREAM
		}.andExpect { status { isNotFound() } }
		mockMvc.get("/groups/$group/messages/stream").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `a new message reaches everyone listening right away`() {
		val marekListens = listen(marek)
		val agataListens = listen(agata)

		send(agata, "Book club on Thursday?")

		for (stream in listOf(marekListens, agataListens)) {
			val events = stream.response.contentAsString
			assertContains(events, "event:message")
			assertContains(events, "Book club on Thursday?")
			assertContains(events, "\"name\":\"Agata\"")
		}
	}

	@Test
	fun `someone removed from the group stops hearing it, but their messages stay`() {
		val marekListens = listen(marek)
		send(marek, "Bye everyone")

		mockMvc.delete("/me/groups/$group") { bearer(marek) }.andExpect { status { isNoContent() } }
		send(agata, "Now without Marek")

		val heard = marekListens.response.contentAsString
		assertContains(heard, "Bye everyone")
		assertFalse("Now without Marek" in heard, heard)
		// The stream was closed, so the app finds out and stops. (Waits for the end – throws if it doesn't come.)
		marekListens.getAsyncResult(1000)

		mockMvc.get("/groups/$group/messages") { bearer(agata) }.andExpect {
			jsonPath("$[1].content") { value("Bye everyone") }
			jsonPath("$[1].sender.name") { value("Marek") }
		}
		mockMvc.get("/groups/$group/messages") { bearer(marek) }.andExpect { status { isNotFound() } }
	}

	// Opens the live stream. It stays open, and what the server writes to it shows up in the response.
	private fun listen(token: String): MvcResult =
		mockMvc.get("/groups/$group/messages/stream") {
			bearer(token)
			accept = MediaType.TEXT_EVENT_STREAM
		}.andExpect {
			request { asyncStarted() }
		}.andReturn()

	private fun send(token: String, content: String) =
		mockMvc.post("/groups/$group/messages") { bearer(token); json("""{"content": "$content"}""") }

	private fun field(result: ResultActionsDsl, name: String): String =
		Regex(""""$name":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1]

	private fun registerAndLogIn(name: String, email: String): String {
		mockMvc.post("/auth/register") {
			json("""{"name": "$name", "username": "${email.substringBefore('@')}", "email": "$email", "password": "secret-password"}""")
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
