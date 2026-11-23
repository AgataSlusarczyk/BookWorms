package com.bookworms.server.friends

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// A friend request or an accepted friendship between two users.
@Entity
@Table(name = "friendships")
class Friendship(
	val requesterId: UUID,
	val addresseeId: UUID,
	@Enumerated(EnumType.STRING)
	var status: FriendshipStatus = FriendshipStatus.PENDING,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	val createdAt: Instant = Instant.now()
}

enum class FriendshipStatus { PENDING, ACCEPTED, REJECTED }
