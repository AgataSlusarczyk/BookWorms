package com.bookworms.server.groups.dto

import java.time.Instant
import java.util.UUID

// Who to invite: their id, from search.
data class InviteUserRequest(
	val userId: UUID,
)

// An invitation waiting for the user's answer.
data class GroupInvitationResponse(
	val id: UUID,
	val groupId: UUID,
	val groupName: String,
	val inviterName: String,
	val createdAt: Instant,
)
