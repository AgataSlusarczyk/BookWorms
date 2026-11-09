package com.bookworms.server.loans

import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.copies.CopyService
import com.bookworms.server.loans.dto.QueueResponse
import com.bookworms.server.loans.dto.RequestBookResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// The queue for a book that can't be borrowed right now (lent out or not available).
// It's only a "let me know": when the book is available again, the people waiting send a normal request.
@Service
class QueueService(
	private val queueRepository: QueueRepository,
	private val copyService: CopyService,
	private val copyRepository: BookCopyRepository,
	private val borrowingRepository: BorrowingRepository,
) {

	// Joining twice is fine – you just get your place back.
	@Transactional
	fun join(userId: UUID, copyId: UUID): QueueResponse {
		val copy = copyService.othersCopy(userId, copyId)
		val canAskNow = copy.availability == Availability.AVAILABLE &&
			!borrowingRepository.existsByCopyIdAndStatusNot(copyId, BorrowingStatus.RETURNED)
		if (canAskNow) {
			throw ApiException(HttpStatus.CONFLICT, "COPY_AVAILABLE", "This book is available – ask to borrow it instead")
		}
		val id = QueueEntryId(userId = userId, copyId = copyId)
		val entry = queueRepository.findById(id).orElseGet { queueRepository.save(QueueEntry(id)) }
		return describe(entry)
	}

	// Leaving a queue you're not in does nothing.
	@Transactional
	fun leave(userId: UUID, copyId: UUID) {
		queueRepository.deleteById(QueueEntryId(userId = userId, copyId = copyId))
	}

	@Transactional(readOnly = true)
	fun myQueues(userId: UUID): List<QueueResponse> =
		queueRepository.findById_UserIdOrderByCreatedAtAsc(userId).map { describe(it) }

	private fun describe(entry: QueueEntry): QueueResponse {
		val copy = copyRepository.findWithBookById(entry.id.copyId)!!
		return QueueResponse(
			copyId = copy.id!!,
			position = queueRepository.countById_CopyIdAndCreatedAtLessThanEqual(copy.id!!, entry.createdAt),
			book = RequestBookResponse(
				id = copy.book.id!!,
				isbn = copy.book.isbn,
				title = copy.book.title,
				authors = copy.book.authors,
				coverUrl = copy.book.coverUrl,
			),
		)
	}
}
