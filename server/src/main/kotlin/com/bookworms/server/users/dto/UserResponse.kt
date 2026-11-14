package com.bookworms.server.users.dto

import com.bookworms.server.users.User
import java.util.UUID

data class UserResponse(
	val id: UUID,
	val name: String,
	val username: String,
	val email: String,
	// A path on this server, e.g. "/photos/<file>.jpg". The app adds the server address in front of it.
	val photoUrl: String?,
)

fun User.toResponse() = UserResponse(
	id = id!!,
	name = name,
	username = username,
	email = email,
	photoUrl = photoUrl,
)
