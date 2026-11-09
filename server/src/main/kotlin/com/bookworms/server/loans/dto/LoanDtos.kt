package com.bookworms.server.loans.dto

import com.bookworms.server.loans.BorrowingStatus
import java.time.Instant
import java.util.UUID

// One loan, for the Borrowing and Lent tabs.
data class LoanResponse(
	val id: UUID,
	val status: BorrowingStatus,
	// When the owner accepted – "since" in the app.
	val since: Instant,
	val dueDate: Instant,
	val copyId: UUID,
	val book: RequestBookResponse,
	// Who has the book.
	val borrower: PersonResponse,
	// Whose book it is.
	val owner: PersonResponse,
)
