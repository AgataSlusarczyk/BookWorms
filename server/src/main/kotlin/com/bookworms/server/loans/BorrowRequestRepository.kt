package com.bookworms.server.loans

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BorrowRequestRepository : JpaRepository<BorrowRequest, UUID> {

	// Each request comes with its copy and book, because the lists show the title.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findWithCopyById(id: UUID): BorrowRequest?

	// "Waiting for an answer": what I asked for.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findByRequesterIdAndStatusOrderByCreatedAtDesc(requesterId: UUID, status: RequestStatus): List<BorrowRequest>

	// Requests for my books.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findByCopyOwnerIdAndStatusOrderByCreatedAtDesc(ownerId: UUID, status: RequestStatus): List<BorrowRequest>

	fun findByRequesterIdAndIdempotencyKey(requesterId: UUID, idempotencyKey: String): BorrowRequest?

	fun existsByCopyIdAndRequesterIdAndStatus(copyId: UUID, requesterId: UUID, status: RequestStatus): Boolean
}
