package com.bookworms.server.loans.dto

import java.util.UUID

// My place in one copy's queue – "In queue #2" in the app.
data class QueueResponse(
	val copyId: UUID,
	val position: Long,
	val book: RequestBookResponse,
)
