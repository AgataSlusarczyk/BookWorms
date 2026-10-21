package com.bookworms.server.auth

import com.bookworms.server.auth.dto.LoginRequest
import com.bookworms.server.auth.dto.LoginResponse
import com.bookworms.server.auth.dto.RegisterRequest
import com.bookworms.server.users.dto.UserResponse
import com.bookworms.server.users.dto.toResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
	private val authService: AuthService,
) {

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	fun register(@Valid @RequestBody request: RegisterRequest): UserResponse =
		authService.register(request).toResponse()

	@PostMapping("/login")
	fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
		authService.login(request)
}
