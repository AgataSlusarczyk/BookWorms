package com.bookworms.server.users.dto

import com.bookworms.server.users.User
import java.util.UUID

data class UserResponse(
	val id: UUID,
	val name: String,
	val email: String,
)

fun User.toResponse() = UserResponse(
	id = id!!,
	name = name,
	email = email,
)
