package com.bookworms.server.friends.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class SendFriendRequest(
	@field:NotBlank
	@field:Email
	val email: String,
)

data class FriendResponse(
	val id: UUID,
	val name: String,
	val username: String,
	val email: String,
	val photoUrl: String? = null,
)   

data class FriendRequestResponse(
	val id: UUID,
	val requesterId: UUID,
	val requesterName: String,
	val requesterEmail: String,
)   