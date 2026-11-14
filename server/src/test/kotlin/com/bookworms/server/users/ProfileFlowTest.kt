package com.bookworms.server.users

import com.bookworms.server.TestcontainersConfiguration
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
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
