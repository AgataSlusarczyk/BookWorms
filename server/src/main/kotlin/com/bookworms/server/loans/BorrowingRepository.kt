package com.bookworms.server.loans

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BorrowingRepository : JpaRepository<Borrowing, UUID> {

	// Is the copy lent out right now?
	fun existsByCopyIdAndStatusNot(copyId: UUID, status: BorrowingStatus): Boolean

	// Each loan comes with its copy and book, because the lists show the title.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findWithCopyById(id: UUID): Borrowing?

	// "Borrowing": books I have from other people. The one to give back soonest first.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findByBorrowerIdAndStatusNotOrderByDueDateAsc(borrowerId: UUID, status: BorrowingStatus): List<Borrowing>

	// "Lent": my books other people have.
	@EntityGraph(attributePaths = ["copy", "copy.book"])
	fun findByCopyOwnerIdAndStatusNotOrderByDueDateAsc(ownerId: UUID, status: BorrowingStatus): List<Borrowing>
}
