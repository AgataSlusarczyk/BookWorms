package com.bookworms.server.chat

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

// One message in a group's chat.
@Entity
@Table(name = "group_messages")
class GroupMessage(
	val groupId: UUID,
	val senderId: UUID,
	val content: String,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// Cut to microseconds, like Postgres keeps it, so the time the app gets is exactly the one in the database
	// (older pages are asked for "before this message", see GroupMessageRepository).
	val createdAt: Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)
}
