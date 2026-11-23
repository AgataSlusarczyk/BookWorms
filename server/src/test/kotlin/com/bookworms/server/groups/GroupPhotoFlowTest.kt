package com.bookworms.server.groups

import com.bookworms.server.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertNotEquals

// The group's photo, and people's photos wherever the group shows who's who.
// Agata makes "Czytelnia" and is its admin, Marek joins it, Zosia stays outside.
// Photos go to a folder under build/, like in ProfileFlowTest.
@SpringBootTest(properties = ["bookworms.photos.dir=build/test-photos"])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class GroupPhotoFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var zosia: String
	private lateinit var group: String
	private lateinit var code: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		zosia = registerAndLogIn("Zosia", "zosia@example.com")
		val created = mockMvc.post("/groups") {
			bearer(agata)
			json("""{"name": "Czytelnia"}""")
		}.andExpect {
			status { isCreated() }
			jsonPath("$.photoUrl") { doesNotExist() }
		}
		group = field(created, "id")
		code = field(created, "inviteCode")
		mockMvc.post("/groups/join") { bearer(marek); json("""{"code": "$code"}""") }
			.andExpect { status { isOk() } }
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf("group_messages", "group_members", "groups", "users")) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `the admin's photo shows on the group everywhere, and a new one replaces it`() {
		val first = field(uploadGroupPhoto(agata, picture(100, 100)).andExpect { status { isOk() } }, "photoUrl")
		assert(first.startsWith("/photos/") && first.endsWith(".jpg")) { first }
		// Loaded without the token, like the phone's image loader does.
		mockMvc.get(first).andExpect {
			status { isOk() }
			content { contentType("image/jpeg") }
		}

		mockMvc.get("/groups/$group") { bearer(marek) }.andExpect { jsonPath("$.photoUrl") { value(first) } }
		mockMvc.get("/me/groups") { bearer(marek) }.andExpect { jsonPath("$[0].photoUrl") { value(first) } }
		mockMvc.post("/groups/invite") { bearer(zosia); json("""{"code": "$code"}""") }
			.andExpect { jsonPath("$.photoUrl") { value(first) } }

		val second = field(uploadGroupPhoto(agata, picture(50, 50)), "photoUrl")
		assertNotEquals(first, second)
		// The old file is gone.
		mockMvc.get(first).andExpect { status { isNotFound() } }
	}

	@Test
	fun `a removed group photo is gone`() {
		val url = field(uploadGroupPhoto(agata, picture(100, 100)), "photoUrl")
		mockMvc.delete("/groups/$group/photo") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.photoUrl") { doesNotExist() }
		}
		mockMvc.get(url).andExpect { status { isNotFound() } }
	}

	@Test
	fun `only the admin changes the photo, and someone outside doesn't see the group`() {
		uploadGroupPhoto(marek, picture(10, 10)).andExpect {
			status { isForbidden() }
			jsonPath("$.code") { value("NOT_GROUP_ADMIN") }
		}
		mockMvc.delete("/groups/$group/photo") { bearer(marek) }.andExpect { status { isForbidden() } }
		uploadGroupPhoto(zosia, picture(10, 10)).andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("GROUP_NOT_FOUND") }
		}
	}

	@Test
	fun `something that isn't a picture returns 400`() {
		uploadGroupPhoto(agata, "not a picture".toByteArray()).andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("INVALID_PHOTO") }
		}
	}

	@Test
	fun `deleting the group removes its photo`() {
		val url = field(uploadGroupPhoto(agata, picture(100, 100)), "photoUrl")
		mockMvc.delete("/groups/$group") { bearer(agata) }.andExpect { status { isNoContent() } }
		mockMvc.get(url).andExpect { status { isNotFound() } }
	}

	@Test
	fun `members and chat messages come with the person's photo`() {
		val marekPhoto = field(
			mockMvc.multipart(HttpMethod.PUT, "/me/photo") {
				bearer(marek)
				file(MockMultipartFile("photo", "me.png", "image/png", picture(100, 100)))
			}.andExpect { status { isOk() } },
			"photoUrl",
		)

		mockMvc.get("/groups/$group") { bearer(agata) }.andExpect {
			// Agata first – she was there before Marek. She has no photo.
			jsonPath("$.members[0].photoUrl") { doesNotExist() }
			jsonPath("$.members[1].name") { value("Marek") }
			jsonPath("$.members[1].photoUrl") { value(marekPhoto) }
		}

		mockMvc.post("/groups/$group/messages") { bearer(marek); json("""{"content": "Hi!"}""") }
			.andExpect { status { isCreated() } }
		mockMvc.get("/groups/$group/messages") { bearer(agata) }.andExpect {
			jsonPath("$[0].sender.name") { value("Marek") }
			jsonPath("$[0].sender.photoUrl") { value(marekPhoto) }
		}
	}

	private fun uploadGroupPhoto(token: String, bytes: ByteArray) =
		mockMvc.multipart(HttpMethod.PUT, "/groups/$group/photo") {
			bearer(token)
			file(MockMultipartFile("photo", "group.png", "image/png", bytes))
		}

	// A PNG of this size, like one picked from the phone's gallery.
	private fun picture(width: Int, height: Int): ByteArray {
		val out = ByteArrayOutputStream()
		ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out)
		return out.toByteArray()
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
