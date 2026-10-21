package com.bookworms.server.auth.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
	@NotBlank
	val email: String,

	@NotBlank
	val password: String,
)
