package com.bookworms.server.users.dto

import com.bookworms.server.users.USERNAME_PATTERN
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// Edit profile sends all three, changed or not.
data class UpdateProfileRequest(
	@NotBlank
	@Size(max = 100)
	val name: String,

	@Pattern(regexp = USERNAME_PATTERN)
	val username: String,

	@NotBlank
	@Email
	@Size(max = 255)
	val email: String,
)
