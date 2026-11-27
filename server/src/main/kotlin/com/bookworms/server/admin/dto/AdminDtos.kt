package com.bookworms.server.admin.dto

import com.bookworms.server.admin.AdminAction
import com.bookworms.server.admin.AdminTarget
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.users.GlobalRole
import com.bookworms.server.users.UserStatus
import java.time.Instant
import java.util.UUID

// The numbers at the top of the admin panel.
data class AdminStatsResponse(
	val users: Long,
	val blockedUsers: Long,
	val books: Long,
	// Copies people have now – removed ones don't count.
	val copies: Long,
	val groups: Long,
	// Visible ones – hidden ones don't count.
	val reviews: Long,
	// Loans that aren't RETURNED yet.
	val activeLoans: Long,
)

// One account on the admin panel's list. Unlike everywhere else, with the email – the admin needs to tell people apart.
data class AdminUserResponse(
	val id: UUID,
	val name: String,
	val username: String,
	val email: String,
	val photoUrl: String?,
	val role: GlobalRole,
	val status: UserStatus,
	val createdAt: Instant,
)

// One group on the admin panel's list. The admin isn't in most of them, so this is all they see of it.
data class AdminGroupResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val photoUrl: String?,
	val memberCount: Long,
	val createdBy: PersonResponse,
	val createdAt: Instant,
)

data class AdminLogResponse(
	val id: UUID,
	val admin: PersonResponse,
	val action: AdminAction,
	val targetType: AdminTarget,
	val targetId: UUID,
	val description: String?,
	val createdAt: Instant,
)
