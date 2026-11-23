package com.bookworms.server.groups

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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

// Inviting someone to a group by their username, and their answer.
// Agata makes "Czytelnia" and is its admin, Marek joins it with the code, Zosia gets invited.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class GroupInvitationFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var zosia: String
	private lateinit var zosiaId: String
	private lateinit var group: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		zosia = registerAndLogIn("Zosia", "zosia@example.com")
		zosiaId = field(mockMvc.get("/me") { bearer(zosia) }, "id")
		val created = mockMvc.post("/groups") { bearer(agata); json("""{"name": "Czytelnia"}""") }
		group = field(created, "id")
		val code = field(created, "inviteCode")
		mockMvc.post("/groups/join") { bearer(marek); json("""{"code": "$code"}""") }
			.andExpect { status { isOk() } }
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf("friendships", "group_invitations", "group_messages", "group_members", "groups", "users")) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `search leaves out members, and an accepted invitation makes a member`() {
		mockMvc.get("/groups/$group/search-users?query=zo") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].id") { value(zosiaId) }
			jsonPath("$[0].email") { doesNotExist() }
		}
		// Marek is in the group already.
		mockMvc.get("/groups/$group/search-users?query=ma") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }

		invite(agata, zosiaId).andExpect { status { isCreated() } }
		// Invited already – not in search anymore.
		mockMvc.get("/groups/$group/search-users?query=zo") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }

		val invitationId = field(
			mockMvc.get("/me/group-invitations") { bearer(zosia) }.andExpect {
				jsonPath("$.length()") { value(1) }
				jsonPath("$[0].groupName") { value("Czytelnia") }
				jsonPath("$[0].inviterName") { value("Agata") }
			},
			"id",
		)
		mockMvc.post("/me/group-invitations/$invitationId/accept") { bearer(zosia) }.andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Czytelnia") }
			jsonPath("$.members.length()") { value(3) }
		}
		mockMvc.get("/me/group-invitations") { bearer(zosia) }.andExpect { jsonPath("$.length()") { value(0) } }
		invite(agata, zosiaId).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("ALREADY_MEMBER") }
		}
	}

	@Test
	fun `one invitation at a time, and after a no a new one can be sent`() {
		invite(agata, zosiaId).andExpect { status { isCreated() } }
		invite(agata, zosiaId).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("INVITATION_EXISTS") }
		}
		val invitationId = field(mockMvc.get("/me/group-invitations") { bearer(zosia) }, "id")
		// Only Zosia answers her invitation.
		mockMvc.post("/me/group-invitations/$invitationId/accept") { bearer(marek) }.andExpect { status { isNotFound() } }

		mockMvc.post("/me/group-invitations/$invitationId/reject") { bearer(zosia) }.andExpect { status { isNoContent() } }
		mockMvc.get("/groups/$group") { bearer(zosia) }.andExpect { status { isNotFound() } }
		invite(agata, zosiaId).andExpect { status { isCreated() } }
	}

	@Test
	fun `members invite only when the admin lets them, and outsiders can't`() {
		mockMvc.get("/groups/$group/search-users?query=zo") { bearer(marek) }.andExpect {
			status { isForbidden() }
			jsonPath("$.code") { value("NOT_GROUP_ADMIN") }
		}
		invite(marek, zosiaId).andExpect { status { isForbidden() } }
		invite(zosia, zosiaId).andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("GROUP_NOT_FOUND") }
		}

		mockMvc.patch("/groups/$group") { bearer(agata); json("""{"name": "Czytelnia", "membersCanInvite": true}""") }
			.andExpect { status { isOk() } }
		invite(marek, zosiaId).andExpect { status { isCreated() } }
		mockMvc.get("/me/group-invitations") { bearer(zosia) }.andExpect { jsonPath("$[0].inviterName") { value("Marek") } }
	}

	private fun invite(token: String, userId: String) =
		mockMvc.post("/groups/$group/invitations") {
			bearer(token)
			json("""{"userId": "$userId"}""")
		}

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

	// The first "name": "value" in the answer – the group's own fields come before its members'.
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
