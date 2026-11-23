package com.bookworms.server.groups

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GroupInvitationRepository : JpaRepository<GroupInvitation, UUID> {
	fun existsByGroupIdAndInviteeIdAndStatus(
		groupId: UUID,
		inviteeId: UUID,
		status: GroupInvitationStatus,
	): Boolean

	fun findByInviteeIdAndStatusOrderByCreatedAtDesc(
		inviteeId: UUID,
		status: GroupInvitationStatus,
	): List<GroupInvitation>

	fun findByGroupIdAndStatus(
		groupId: UUID,
		status: GroupInvitationStatus,
	): List<GroupInvitation>
}
