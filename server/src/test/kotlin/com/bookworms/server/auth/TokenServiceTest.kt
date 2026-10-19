package com.bookworms.server.auth

import com.bookworms.server.common.SecurityConfig
import com.bookworms.server.users.User
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TokenServiceTest {

	private val securityConfig = SecurityConfig()
	private val properties = JwtProperties(secret = "test-secret-that-is-at-least-32-bytes-long", expiration = Duration.ofDays(7))
	private val secretKey = securityConfig.jwtSecretKey(properties)
	private val tokenService = TokenService(securityConfig.jwtEncoder(secretKey), properties)
	private val jwtDecoder = securityConfig.jwtDecoder(secretKey)

	@Test
	fun `issued token is accepted and contains user id`() {
		val user = User(name = "Agata", email = "agata@example.com", passwordHash = "hash")
		user.id = UUID.randomUUID()

		val jwt = jwtDecoder.decode(tokenService.createToken(user))

		assertEquals(user.id.toString(), jwt.subject)
	}

	@Test
	fun `token signed with another secret is rejected`() {
		val otherProperties = properties.copy(secret = "another-secret-that-is-at-least-32-bytes")
		val otherConfig = SecurityConfig()
		val otherTokenService = TokenService(otherConfig.jwtEncoder(otherConfig.jwtSecretKey(otherProperties)), otherProperties)
		val user = User(name = "Agata", email = "agata@example.com", passwordHash = "hash")
		user.id = UUID.randomUUID()

		assertFailsWith<JwtException> {
			jwtDecoder.decode(otherTokenService.createToken(user))
		}
	}
}
