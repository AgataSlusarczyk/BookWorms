package com.bookworms.server.loans

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BorrowingRepository : JpaRepository<Borrowing, UUID> {

	// Is the copy lent out right now?
	fun existsByCopyIdAndStatusNot(copyId: UUID, status: BorrowingStatus): Boolean
}
