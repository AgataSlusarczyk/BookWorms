package com.bookworms.server.groups

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class GroupInvitationStatus {
	PENDING,
	ACCEPTED,
	REJECTED,
}

@Entity
@Table(name = "group_invitations")
class GroupInvitation(
	@Column(name = "group_id", nullable = false)
	val groupId: UUID,

	@Column(name = "inviter_id", nullable = false)
	val inviterId: UUID,

	@Column(name = "invitee_id", nullable = false)
	val inviteeId: UUID,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: GroupInvitationStatus = GroupInvitationStatus.PENDING,

	@Column(name = "created_at", nullable = false)
	val createdAt: Instant = Instant.now(),

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,
)