package com.bookworms.server.loans

import com.bookworms.server.copies.BookCopy
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// A loan: made when the owner accepts a request. A copy has at most one loan that isn't RETURNED yet.
@Entity
@Table(name = "borrowings")
class Borrowing(
	val requestId: UUID,
	// The lists show the book's title and whose it is, so the loan points at the copy itself.
	@field:ManyToOne(fetch = FetchType.LAZY, optional = false)
	@field:JoinColumn(name = "copy_id")
	val copy: BookCopy,
	val borrowerId: UUID,
	var dueDate: Instant,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// Starts as BORROWED, like in the app: accepting means the book is lent.
	// The database also allows both people confirming the handover and the return – not used yet.
	@Enumerated(EnumType.STRING)
	var status: BorrowingStatus = BorrowingStatus.BORROWED

	// When the owner marked the book as back.
	var returnConfirmedByOwnerAt: Instant? = null

	val createdAt: Instant = Instant.now()

	var updatedAt: Instant = Instant.now()

	@PreUpdate
	fun touch() {
		updatedAt = Instant.now()
	}
}

enum class BorrowingStatus { PENDING_HANDOVER, BORROWED, PENDING_RETURN, RETURNED }
