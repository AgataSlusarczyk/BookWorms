package com.bookworms.server.users

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
	var name: String,
	var email: String,
	var passwordHash: String,
	// Always in lower case.
	var username: String,
	// A path on this server, e.g. "/photos/<file>.jpg", or null when there's no photo.
	var photoUrl: String? = null,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	val createdAt: Instant = Instant.now()

	// SYSTEM_ADMIN opens the admin panel and can do more on the other screens (see AdminService).
	// Not the same as a group's admin (GroupRole), who only looks after one group.
	@Enumerated(EnumType.STRING)
	var globalRole: GlobalRole = GlobalRole.USER

	// A BLOCKED user can't log in, and the token they already have stops working (see BlockedUserFilter).
	// Users are never deleted, only blocked.
	@Enumerated(EnumType.STRING)
	var status: UserStatus = UserStatus.ACTIVE

	val isAdmin: Boolean get() = globalRole == GlobalRole.SYSTEM_ADMIN && status == UserStatus.ACTIVE
}

enum class GlobalRole { USER, SYSTEM_ADMIN }

enum class UserStatus { ACTIVE, BLOCKED }
