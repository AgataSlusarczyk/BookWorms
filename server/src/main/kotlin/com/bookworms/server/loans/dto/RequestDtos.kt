package com.bookworms.server.loans.dto

import com.bookworms.server.loans.RequestStatus
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// What "Borrow" sends. Optional: the app makes one key per tap, so sending it again doesn't make a second request.
data class SendRequestRequest(
	@field:Size(max = 100)
	val idempotencyKey: String? = null,
)

// One request, for Loans and Notifications.
data class RequestResponse(
	val id: UUID,
	val status: RequestStatus,
	val createdAt: Instant,
	val copyId: UUID,
	val book: RequestBookResponse,
	// Who asked.
	val requester: PersonResponse,
	// Whose book it is.
	val owner: PersonResponse,
	// Only for the owner: false while the book is lent out or not available, so the app grays out Accept.
	val canAccept: Boolean,
)

data class RequestBookResponse(
	val id: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val coverUrl: String?,
)

data class PersonResponse(
	val id: UUID,
	val name: String,
)
