package com.bookworms.server.users

import com.bookworms.server.users.dto.UpdateProfileRequest
import com.bookworms.server.users.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class UserController(
	private val userService: UserService,
) {

	@GetMapping("/me")
	fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse =
		userService.me(UUID.fromString(jwt.subject))

	// Edit profile: name, username and email.
	@PutMapping("/me")
	fun updateProfile(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: UpdateProfileRequest): UserResponse =
		userService.updateProfile(UUID.fromString(jwt.subject), request)
}
