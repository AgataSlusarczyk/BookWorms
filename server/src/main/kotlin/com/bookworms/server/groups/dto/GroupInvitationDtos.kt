package com.bookworms.server.groups.dto

import java.time.Instant
import java.util.UUID

data class InviteUserRequest(
	val userId: UUID,
)

data class GroupInvitationResponse(
	val id: UUID,
	val groupId: UUID,
	val groupName: String,
	val inviterName: String,
	val createdAt: Instant,
)