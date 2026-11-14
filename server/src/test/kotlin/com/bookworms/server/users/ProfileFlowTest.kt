package com.bookworms.server.users

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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// Photos go to a folder under build/, so tests don't fill the real uploads/ folder.
@SpringBootTest(properties = ["bookworms.photos.dir=build/test-photos"])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class ProfileFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val userRepository: UserRepository,
) {

	private lateinit var agata: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata", "agata@example.com")
		registerAndLogIn("Marek", "marek", "marek@example.com")
	}

	// The other test classes share this database.
	@AfterEach
	fun cleanDatabase() {
		userRepository.deleteAllInBatch()
	}

	@Test
	fun `changing the profile needs logging in`() {
		mockMvc.put("/me") {
			json("""{"name": "Agata", "username": "agata", "email": "agata@example.com"}""")
		}.andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `the new name, username and email are saved, and the new email logs in`() {
		updateProfile(agata, """{"name": " Agata S ", "username": "Agata_S", "email": "Agata.S@Example.com"}""")
			.andExpect {
				status { isOk() }
				jsonPath("$.name") { value("Agata S") }
				jsonPath("$.username") { value("agata_s") }
				jsonPath("$.email") { value("agata.s@example.com") }
			}

		// The old token still works – it holds only the user's id.
		mockMvc.get("/me") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.username") { value("agata_s") }
		}
		mockMvc.post("/auth/login") {
			json("""{"email": "agata.s@example.com", "password": "secret-password"}""")
		}.andExpect { status { isOk() } }
	}

	@Test
	fun `keeping my own username and email is fine`() {
		updateProfile(agata, """{"name": "Agata S", "username": "AGATA", "email": "agata@example.com"}""")
			.andExpect {
				status { isOk() }
				jsonPath("$.name") { value("Agata S") }
			}
	}

	@Test
	fun `someone else's email returns 409`() {
		updateProfile(agata, """{"name": "Agata", "username": "agata", "email": "MAREK@example.com"}""")
			.andExpect {
				status { isConflict() }
				jsonPath("$.code") { value("EMAIL_TAKEN") }
			}
	}

	@Test
	fun `someone else's username returns 409`() {
		updateProfile(agata, """{"name": "Agata", "username": "Marek", "email": "agata@example.com"}""")
			.andExpect {
				status { isConflict() }
				jsonPath("$.code") { value("USERNAME_TAKEN") }
			}
	}

	@Test
	fun `a blank name, a wrong username or a wrong email returns 400`() {
		listOf(
			"""{"name": "  ", "username": "agata", "email": "agata@example.com"}""",
			"""{"name": "Agata", "username": "ag", "email": "agata@example.com"}""",
			"""{"name": "Agata", "username": "agata!", "email": "agata@example.com"}""",
			"""{"name": "Agata", "username": "agata", "email": "not-an-email"}""",
		).forEach { body ->
			updateProfile(agata, body).andExpect { status { isBadRequest() } }
		}
	}

	@Test
	fun `a photo is saved as a small JPEG that anyone can load, and a new one replaces it`() {
		val first = photoUrl(uploadPhoto(agata, picture(2000, 1000)).andExpect { status { isOk() } })
		assert(first.startsWith("/photos/") && first.endsWith(".jpg")) { first }

		// Loaded without the token, like the phone's image loader does.
		val saved = mockMvc.get(first).andExpect {
			status { isOk() }
			content { contentType("image/jpeg") }
		}.andReturn().response.contentAsByteArray
		val image = ImageIO.read(ByteArrayInputStream(saved))
		assertEquals(512, image.width)
		assertEquals(256, image.height)

		val second = photoUrl(uploadPhoto(agata, picture(100, 100)))
		assertNotEquals(first, second)
		// The old file is gone.
		mockMvc.get(first).andExpect { status { isNotFound() } }
		mockMvc.get("/me") { bearer(agata) }.andExpect { jsonPath("$.photoUrl") { value(second) } }
	}

	@Test
	fun `a removed photo is gone`() {
		val url = photoUrl(uploadPhoto(agata, picture(100, 100)))
		mockMvc.delete("/me/photo") { bearer(agata) }.andExpect {
			status { isOk() }
			jsonPath("$.photoUrl") { doesNotExist() }
		}
		mockMvc.get(url).andExpect { status { isNotFound() } }
	}

	@Test
	fun `something that isn't a picture returns 400, and a photo needs logging in`() {
		uploadPhoto(agata, "not a picture".toByteArray()).andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("INVALID_PHOTO") }
		}
		mockMvc.multipart(HttpMethod.PUT, "/me/photo") {
			file(MockMultipartFile("photo", "me.png", "image/png", picture(10, 10)))
		}.andExpect { status { isUnauthorized() } }
	}

	private fun uploadPhoto(token: String, bytes: ByteArray) =
		mockMvc.multipart(HttpMethod.PUT, "/me/photo") {
			bearer(token)
			file(MockMultipartFile("photo", "me.png", "image/png", bytes))
		}

	// A PNG of this size, like one picked from the phone's gallery.
	private fun picture(width: Int, height: Int): ByteArray {
		val out = ByteArrayOutputStream()
		ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out)
		return out.toByteArray()
	}

	private fun photoUrl(result: ResultActionsDsl): String =
		Regex(""""photoUrl":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1]

	private fun updateProfile(token: String, body: String) =
		mockMvc.put("/me") {
			bearer(token)
			json(body)
		}

	private fun registerAndLogIn(name: String, username: String, email: String): String {
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
