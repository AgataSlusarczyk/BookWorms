package com.bookworms.server.notifications

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// One notification for one person. The app builds the text from the type, the book and the other person.
@Entity
@Table(name = "notifications")
class Notification(
	// Who gets it.
	val userId: UUID,
	@Enumerated(EnumType.STRING)
	val type: NotificationType,
	// What it's about – see NotificationType.
	val referenceId: UUID,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	var isRead: Boolean = false

	val createdAt: Instant = Instant.now()
}

// The comment says what referenceId points at.
enum class NotificationType {
	BORROW_REQUESTED, // request – someone asks for my book
	REQUEST_ACCEPTED, // request – the owner said yes
	REQUEST_REJECTED, // request – the owner said no, or removed the book
	REQUEST_SUPERSEDED, // not used
	REQUEST_CANCELED, // request – the person who asked took it back
	HANDOVER_CONFIRMED, // not used yet
	RETURN_CONFIRMED, // borrowing – the owner marked the book as returned
	RETURN_DUE, // not used yet
	RETURN_OVERDUE, // not used yet
	COPY_AVAILABLE, // copy – a book I queued for is available again
}
