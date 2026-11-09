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

// "Can I borrow your book?" – always for one copy. Many people can ask for the same copy;
// the owner accepts one, and the others wait until it's back (or the owner rejects them).
@Entity
@Table(name = "borrow_requests")
class BorrowRequest(
	@field:ManyToOne(fetch = FetchType.LAZY, optional = false)
	@field:JoinColumn(name = "copy_id")
	val copy: BookCopy,
	val requesterId: UUID,
	// Sent again by the app when the internet was slow, so a double tap makes one request, not two.
	val idempotencyKey: String,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	@Enumerated(EnumType.STRING)
	var status: RequestStatus = RequestStatus.PENDING

	val createdAt: Instant = Instant.now()

	var updatedAt: Instant = Instant.now()

	@PreUpdate
	fun touch() {
		updatedAt = Instant.now()
	}
}

// REJECTED = the owner said no. CANCELED = the person who asked took it back.
// SUPERSEDED is in the database too, but we don't use it: other requests stay open when one is accepted.
enum class RequestStatus { PENDING, ACCEPTED, REJECTED, CANCELED, SUPERSEDED }
