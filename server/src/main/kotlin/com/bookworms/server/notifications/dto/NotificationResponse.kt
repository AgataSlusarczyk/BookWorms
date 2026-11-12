package com.bookworms.server.notifications.dto

import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.notifications.NotificationType
import java.time.Instant
import java.util.UUID

// One notification, e.g. "Marek asks to borrow Solaris".
data class NotificationResponse(
	val id: UUID,
	val type: NotificationType,
	// The request, loan or copy it's about (see NotificationType).
	val referenceId: UUID,
	val read: Boolean,
	val createdAt: Instant,
	val bookTitle: String,
	// The other person: who asked, who answered, or whose book it is.
	val person: PersonResponse,
)
