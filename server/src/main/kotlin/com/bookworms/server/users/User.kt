package com.bookworms.server.users

import jakarta.persistence.Entity
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
}
