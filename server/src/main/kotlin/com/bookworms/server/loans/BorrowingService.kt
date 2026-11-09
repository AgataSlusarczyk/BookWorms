package com.bookworms.server.loans

import com.bookworms.server.common.ApiException
import com.bookworms.server.loans.dto.LoanResponse
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.loans.dto.RequestBookResponse
import com.bookworms.server.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// Books that are lent out right now, and giving them back.
// Only the owner marks a book as returned – they're the one who got it back.
@Service
class BorrowingService(
	private val borrowingRepository: BorrowingRepository,
	private val userRepository: UserRepository,
) {

	// "Borrowing": books I have from other people.
	@Transactional(readOnly = true)
	fun myBorrowings(userId: UUID): List<LoanResponse> =
		describe(borrowingRepository.findByBorrowerIdAndStatusNotOrderByDueDateAsc(userId, BorrowingStatus.RETURNED))

	// "Lent": my books other people have.
	@Transactional(readOnly = true)
	fun myLent(userId: UUID): List<LoanResponse> =
		describe(borrowingRepository.findByCopyOwnerIdAndStatusNotOrderByDueDateAsc(userId, BorrowingStatus.RETURNED))

	// The book is back, so it can be lent again – a waiting request can now be accepted.
	@Transactional
	fun markReturned(ownerId: UUID, borrowingId: UUID): LoanResponse {
		val borrowing = borrowingRepository.findWithCopyById(borrowingId)
		// Someone else's loan answers the same as a missing one.
		if (borrowing == null || borrowing.copy.ownerId != ownerId) {
			throw ApiException(HttpStatus.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found")
		}
		if (borrowing.status == BorrowingStatus.RETURNED) {
			throw ApiException(HttpStatus.CONFLICT, "LOAN_ALREADY_RETURNED", "This book was already returned")
		}
		borrowing.status = BorrowingStatus.RETURNED
		borrowing.returnConfirmedByOwnerAt = Instant.now()
		return describe(listOf(borrowing)).single()
	}

	// Turns loans into answers for the app, with the people's names.
	private fun describe(borrowings: List<Borrowing>): List<LoanResponse> {
		val names = userRepository.findAllById(borrowings.flatMap { listOf(it.borrowerId, it.copy.ownerId) }.toSet())
			.associate { it.id!! to it.name }
		return borrowings.map { borrowing ->
			val copy = borrowing.copy
			LoanResponse(
				id = borrowing.id!!,
				status = borrowing.status,
				since = borrowing.createdAt,
				dueDate = borrowing.dueDate,
				copyId = copy.id!!,
				book = RequestBookResponse(
					id = copy.book.id!!,
					isbn = copy.book.isbn,
					title = copy.book.title,
					authors = copy.book.authors,
					coverUrl = copy.book.coverUrl,
				),
				borrower = PersonResponse(borrowing.borrowerId, names[borrowing.borrowerId].orEmpty()),
				owner = PersonResponse(copy.ownerId, names[copy.ownerId].orEmpty()),
			)
		}
	}
}
