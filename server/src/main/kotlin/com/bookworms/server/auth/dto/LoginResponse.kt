package com.bookworms.server.auth.dto

import com.bookworms.server.users.dto.UserResponse

data class LoginResponse(
	val token: String,
	val user: UserResponse,
)
