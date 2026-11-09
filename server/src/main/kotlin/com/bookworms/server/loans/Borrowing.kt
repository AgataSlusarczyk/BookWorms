package com.bookworms.server.loans

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// A loan: made when the owner accepts a request. A copy has at most one loan that isn't RETURNED yet.
@Entity
@Table(name = "borrowings")
class Borrowing(
	val requestId: UUID,
	val copyId: UUID,
	val borrowerId: UUID,
	var dueDate: Instant,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// Starts as BORROWED, like in the app: accepting means the book is lent.
	// The database also allows both people confirming the handover (PENDING_HANDOVER) – not used yet.
	@Enumerated(EnumType.STRING)
	var status: BorrowingStatus = BorrowingStatus.BORROWED

	val createdAt: Instant = Instant.now()

	var updatedAt: Instant = Instant.now()

	@PreUpdate
	fun touch() {
		updatedAt = Instant.now()
	}
}

enum class BorrowingStatus { PENDING_HANDOVER, BORROWED, PENDING_RETURN, RETURNED }
