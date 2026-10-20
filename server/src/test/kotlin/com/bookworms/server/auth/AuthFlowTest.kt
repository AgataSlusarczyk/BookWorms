package com.bookworms.server.auth

import com.bookworms.server.TestcontainersConfiguration
import com.bookworms.server.users.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AuthFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val userRepository: UserRepository,
) {

	@BeforeEach
	fun cleanDatabase() {
		userRepository.deleteAll()
	}

	@Test
	fun `user can register, log in and read own profile`() {
		register("Agata", "agata@example.com", "secret-password")

		val loginBody = mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email": "Agata@Example.com", "password": "secret-password"}"""
		}.andExpect {
			status { isOk() }
			jsonPath("$.user.email") { value("agata@example.com") }
		}.andReturn().response.contentAsString
		val token = Regex(""""token":"([^"]+)"""").find(loginBody)!!.groupValues[1]

		mockMvc.get("/me") {
			header("Authorization", "Bearer $token")
		}.andExpect {
			status { isOk() }
			jsonPath("$.name") { value("Agata") }
		}
	}

	@Test
	fun `registering with a taken email returns 409`() {
		register("Agata", "agata@example.com", "secret-password")

		mockMvc.post("/auth/register") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name": "Other", "email": "agata@example.com", "password": "other-password"}"""
		}.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("EMAIL_TAKEN") }
		}
	}

	@Test
	fun `wrong password returns 401`() {
		register("Agata", "agata@example.com", "secret-password")

		mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email": "agata@example.com", "password": "wrong-password"}"""
		}.andExpect {
			status { isUnauthorized() }
			jsonPath("$.code") { value("INVALID_CREDENTIALS") }
		}
	}

	@Test
	fun `me without token returns 401`() {
		mockMvc.get("/me").andExpect {
			status { isUnauthorized() }
		}
	}

	private fun register(name: String, email: String, password: String) {
		mockMvc.post("/auth/register") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name": "$name", "email": "$email", "password": "$password"}"""
		}.andExpect {
			status { isCreated() }
		}
	}
}
