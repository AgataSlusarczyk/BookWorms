package com.bookworms.server.groups.dto

import com.bookworms.server.copies.Availability
import com.bookworms.server.groups.GroupRole
import com.bookworms.server.loans.dto.PersonResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// What Create group and Edit group send.
data class GroupRequest(
	@field:NotBlank
	@field:Size(max = 100)
	val name: String,

	@field:Size(max = 1000)
	val description: String? = null,

	// Only Edit group sends it. Left out = stays as it is.
	val membersCanInvite: Boolean? = null,
)

// "Find group" and "Join" on the Join group screen.
data class JoinGroupRequest(
	@field:NotBlank
	@field:Size(max = 20)
	val code: String,
)

// One group on the Groups list.
data class GroupSummaryResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val memberCount: Long,
	// The logged-in user's role in it.
	val myRole: GroupRole,
)

// Group details: everything about one group, with its members.
data class GroupDetailsResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val myRole: GroupRole,
	val membersCanInvite: Boolean,
	// null when the user can't invite (an ordinary member and the admin didn't allow it).
	val inviteCode: String?,
	// The earliest first.
	val members: List<MemberResponse>,
	val createdAt: Instant,
)

data class MemberResponse(
	val id: UUID,
	val name: String,
	val role: GroupRole,
	val joinedAt: Instant,
)

// What someone sees after typing a code, before joining.
data class InvitePreviewResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val memberCount: Long,
	// true = the user is in it already, so the app can just open it.
	val alreadyMember: Boolean,
)

// One copy in the group's library.
data class GroupBookResponse(
	val copyId: UUID,
	val book: GroupBookInfoResponse,
	val owner: PersonResponse,
	val availability: Availability,
	// Who has it now. null = on the owner's shelf.
	val borrowedBy: PersonResponse?,
)

data class GroupBookInfoResponse(
	val id: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val coverUrl: String?,
)
