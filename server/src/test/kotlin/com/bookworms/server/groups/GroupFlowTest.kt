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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// Making a group, joining it with the code, its books, and looking after it.
// Agata makes "Czytelnia" in every test. Marek and Ola join when a test needs them, Zosia never does.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class GroupFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var ola: String
	private lateinit var zosia: String
	private lateinit var group: String
	private lateinit var code: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		ola = registerAndLogIn("Ola", "ola@example.com")
		zosia = registerAndLogIn("Zosia", "zosia@example.com")
		val created = mockMvc.post("/groups") {
			bearer(agata)
			json("""{"name": "  Czytelnia  ", "description": "Sci-fi from work"}""")
		}.andExpect {
			status { isCreated() }
			jsonPath("$.name") { value("Czytelnia") }
			jsonPath("$.myRole") { value("ADMIN") }
			jsonPath("$.members.length()") { value(1) }
			jsonPath("$.members[0].name") { value("Agata") }
		}
		group = field(created, "id")
		code = field(created, "inviteCode")
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf(
			"availability_watchers", "borrowings", "borrow_requests", "copy_group_visibility", "book_copies",
			"group_members", "groups", "books", "users",
		)) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `groups need logging in`() {
		mockMvc.get("/me/groups").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `the code looks like K7QM-3XTP`() {
		assert(Regex("[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}").matches(code)) { code }
	}

	@Test
	fun `someone with the code sees the group first, then joins it`() {
		// Typed sloppily: small letters, no dash.
		val sloppy = code.lowercase().replace("-", "")
		mockMvc.post("/groups/invite") { bearer(marek); json("""{"code": "$sloppy"}""") }.andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Czytelnia") }
			jsonPath("$.memberCount") { value(1) }
			jsonPath("$.alreadyMember") { value(false) }
		}

		join(marek).andExpect {
			status { isOk() }
			jsonPath("$.myRole") { value("MEMBER") }
			jsonPath("$.members.length()") { value(2) }
		}
		// Joining again changes nothing.
		join(marek).andExpect { jsonPath("$.members.length()") { value(2) } }

		mockMvc.get("/me/groups") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].name") { value("Czytelnia") }
			jsonPath("$[0].memberCount") { value(2) }
			jsonPath("$[0].myRole") { value("MEMBER") }
		}
	}

	@Test
	fun `a wrong code finds nothing`() {
		mockMvc.post("/groups/join") { bearer(marek); json("""{"code": "AAAA-AAAA"}""") }.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("INVITE_NOT_FOUND") }
		}
		mockMvc.post("/groups/invite") { bearer(marek); json("""{"code": "short"}""") }.andExpect {
			status { isNotFound() }
		}
	}

	@Test
	fun `someone outside the group can't see it`() {
		mockMvc.get("/groups/$group") { bearer(zosia) }.andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("GROUP_NOT_FOUND") }
		}
		mockMvc.get("/groups/$group/books") { bearer(zosia) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `members see each other's books, with who has them`() {
		join(marek)
		join(ola)
		// Added before joining and after – both show up.
		val solaris = addCopy(agata, "Solaris")
		addCopy(marek, "Lalka")
		lend(solaris, owner = agata, borrower = ola)

		mockMvc.get("/groups/$group/books") { bearer(ola) }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(2) }
			// Newest first.
			jsonPath("$[0].book.title") { value("Lalka") }
			jsonPath("$[0].owner.name") { value("Marek") }
			jsonPath("$[0].borrowedBy") { doesNotExist() }
			jsonPath("$[1].book.title") { value("Solaris") }
			jsonPath("$[1].borrowedBy.name") { value("Ola") }
		}
	}

	@Test
	fun `books added before joining show up in the group too`() {
		addCopy(marek, "Lalka")
		join(marek)

		mockMvc.get("/groups/$group/books") { bearer(agata) }.andExpect { jsonPath("$[0].book.title") { value("Lalka") } }
	}

	@Test
	fun `the owner picks the groups a book is visible in`() {
		join(marek)
		val solaris = addCopy(agata, "Solaris")
		mockMvc.get("/me/copies") { bearer(agata) }.andExpect { jsonPath("$[0].groupIds[0]") { value(group) } }

		changeGroups(agata, solaris, "[]").andExpect {
			status { isOk() }
			jsonPath("$.groupIds.length()") { value(0) }
		}
		mockMvc.get("/groups/$group/books") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		// Marek can't see it anymore, so he can't ask for it either.
		mockMvc.post("/copies/$solaris/requests") { bearer(marek) }.andExpect { status { isNotFound() } }

		changeGroups(agata, solaris, """["$group"]""").andExpect { jsonPath("$.groupIds[0]") { value(group) } }
		mockMvc.get("/groups/$group/books") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(1) } }
	}

	@Test
	fun `a book can only be shown in the owner's own groups`() {
		val zosiasGroup = field(
			mockMvc.post("/groups") { bearer(zosia); json("""{"name": "Zosia's"}""") },
			"id",
		)
		val solaris = addCopy(agata, "Solaris")

		changeGroups(agata, solaris, """["$zosiasGroup"]""").andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("NOT_GROUP_MEMBER") }
		}
	}

	@Test
	fun `only the admin edits the group, and decides if members can invite`() {
		join(marek)
		mockMvc.get("/groups/$group") { bearer(marek) }.andExpect { jsonPath("$.inviteCode") { doesNotExist() } }
		update(marek, """{"name": "Mine now"}""").andExpect {
			status { isForbidden() }
			jsonPath("$.code") { value("NOT_GROUP_ADMIN") }
		}

		update(agata, """{"name": "Czytelnia 23", "description": "", "membersCanInvite": true}""").andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Czytelnia 23") }
			jsonPath("$.description") { doesNotExist() }
		}
		mockMvc.get("/groups/$group") { bearer(marek) }.andExpect { jsonPath("$.inviteCode") { value(code) } }
	}

	@Test
	fun `a new code turns the old one off`() {
		val newCode = field(mockMvc.post("/groups/$group/invite-code") { bearer(agata) }.andExpect { status { isOk() } }, "inviteCode")
		assertNotEquals(code, newCode)

		join(marek).andExpect { status { isNotFound() } }
		mockMvc.post("/groups/join") { bearer(marek); json("""{"code": "$newCode"}""") }.andExpect { status { isOk() } }
		// An ordinary member can't make codes unless the admin allows it.
		mockMvc.post("/groups/$group/invite-code") { bearer(marek) }.andExpect { status { isForbidden() } }
	}

	@Test
	fun `the admin makes admins and removes members, but not other admins`() {
		join(marek)
		join(ola)
		val marekId = memberId("Marek")
		val olaId = memberId("Ola")
		addCopy(ola, "Lalka")

		mockMvc.post("/groups/$group/members/$marekId/admin") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.members[1].role") { value("ADMIN") }
		}
		mockMvc.delete("/groups/$group/members/$marekId") { bearer(agata) }.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("MEMBER_IS_ADMIN") }
		}
		mockMvc.delete("/groups/$group/members/$olaId") { bearer(ola) }.andExpect { status { isForbidden() } }

		mockMvc.delete("/groups/$group/members/$olaId") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.members.length()") { value(2) }
		}
		// Her books left with her.
		mockMvc.get("/groups/$group/books") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/groups/$group") { bearer(ola) }.andExpect { status { isNotFound() } }
	}

	@Test
	fun `when the last admin leaves, the longest member becomes admin`() {
		join(marek)
		join(ola)

		mockMvc.delete("/me/groups/$group") { bearer(agata) }.andExpect { status { isNoContent() } }

		mockMvc.get("/groups/$group") { bearer(marek) }.andExpect { jsonPath("$.myRole") { value("ADMIN") } }
		mockMvc.get("/groups/$group") { bearer(ola) }.andExpect { jsonPath("$.myRole") { value("MEMBER") } }
		mockMvc.get("/me/groups") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `the last one out deletes the group`() {
		mockMvc.delete("/me/groups/$group") { bearer(agata) }.andExpect { status { isNoContent() } }

		assertEquals(0, jdbc.queryForObject("select count(*) from groups", Int::class.java))
	}

	@Test
	fun `the admin deletes the group for everyone`() {
		join(marek)
		mockMvc.delete("/groups/$group") { bearer(marek) }.andExpect { status { isForbidden() } }

		mockMvc.delete("/groups/$group") { bearer(agata) }.andExpect { status { isNoContent() } }

		mockMvc.get("/me/groups") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	private fun join(token: String) =
		mockMvc.post("/groups/join") { bearer(token); json("""{"code": "$code"}""") }

	private fun update(token: String, body: String) =
		mockMvc.patch("/groups/$group") { bearer(token); json(body) }

	private fun changeGroups(token: String, copyId: String, groupIds: String) =
		mockMvc.put("/copies/$copyId/groups") { bearer(token); json("""{"groupIds": $groupIds}""") }

	// A book typed in by hand, so it doesn't need the catalog.
	private fun addCopy(token: String, title: String): String =
		field(
			mockMvc.post("/me/copies") {
				bearer(token)
				json("""{"title": "$title", "authors": "Someone"}""")
			}.andExpect { status { isCreated() } },
			"id",
		)

	private fun lend(copyId: String, owner: String, borrower: String) {
		val request = field(mockMvc.post("/copies/$copyId/requests") { bearer(borrower) }, "id")
		mockMvc.post("/requests/$request/accept") { bearer(owner) }.andExpect { status { isOk() } }
	}

	private fun memberId(name: String): String {
		val body = mockMvc.get("/groups/$group") { bearer(agata) }.andReturn().response.contentAsString
		return Regex(""""id":"([^"]+)","name":"$name"""").find(body)!!.groupValues[1]
	}

	private fun field(result: ResultActionsDsl, name: String): String =
		Regex(""""$name":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1]

	private fun registerAndLogIn(name: String, email: String): String {
		mockMvc.post("/auth/register") {
			json("""{"name": "$name", "email": "$email", "password": "secret-password"}""")
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
