package com.bookworms.server.users

import com.bookworms.server.users.dto.UserResponse
import com.bookworms.server.users.dto.toResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
class UserController(
	private val userRepository: UserRepository,
) {

	@GetMapping("/me")
	fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse {
		val user = userRepository.findByIdOrNull(UUID.fromString(jwt.subject))
			?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
		return user.toResponse()
	}
}
