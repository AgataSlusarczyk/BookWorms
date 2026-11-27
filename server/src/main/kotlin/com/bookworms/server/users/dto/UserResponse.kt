package com.bookworms.server.users.dto

import com.bookworms.server.users.GlobalRole
import com.bookworms.server.users.User
import java.util.UUID

data class UserResponse(
	val id: UUID,
	val name: String,
	val username: String,
	val email: String,
	// A path on this server, e.g. "/photos/<file>.jpg". The app adds the server address in front of it.
	val photoUrl: String?,
	// SYSTEM_ADMIN = the app shows the admin panel and the admin's buttons on the other screens.
	val role: GlobalRole,
)

fun User.toResponse() = UserResponse(
	id = id!!,
	name = name,
	username = username,
	email = email,
	photoUrl = photoUrl,
	role = globalRole,
)
