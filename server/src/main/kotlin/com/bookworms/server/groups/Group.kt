package com.bookworms.server.groups

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

// Friends who lend each other books – "Czytelnia 23". Members see each other's books.
// Anyone with the invite code can join, right away.
@Entity
@Table(name = "groups")
class Group(
	var name: String,
	var description: String?,
	// E.g. "K7QM-3XTP". A new code replaces the old one, so the old one stops working.
	var inviteCode: String,
	val createdBy: UUID,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// Set by the admin: can ordinary members see the code and invite others too?
	var membersCanInvite: Boolean = false

	val createdAt: Instant = Instant.now()
}

// One person in one group.
@Entity
@Table(name = "group_members")
class GroupMember(
	@EmbeddedId
	val id: GroupMemberId,
	@Enumerated(EnumType.STRING)
	var groupRole: GroupRole = GroupRole.MEMBER,
) {
	val joinedAt: Instant = Instant.now()
}

// The table has no id of its own: a person and a group together are the key, so nobody is in a group twice.
@Embeddable
data class GroupMemberId(
	val userId: UUID,
	val groupId: UUID,
) : Serializable

// An admin edits the group, manages its members and can always invite.
enum class GroupRole { MEMBER, ADMIN }
