package com.bookworms.server.loans

import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopy
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.loans.dto.RequestBookResponse
import com.bookworms.server.loans.dto.RequestResponse
import com.bookworms.server.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

// Asking to borrow a book, and the owner's answer.
// Rules:
//   * you can ask only for a copy that's visible in one of your groups, isn't yours, is AVAILABLE and isn't lent out,
//   * many people can ask for the same copy, but each person only once at a time,
//   * the owner accepts one request – the others stay open, and can be accepted after the book is back,
//   * while the book is lent out, nobody can ask for it and the owner can't accept another request.
@Service
class BorrowRequestService(
	private val requestRepository: BorrowRequestRepository,
	private val copyRepository: BookCopyRepository,
	private val borrowingRepository: BorrowingRepository,
	private val userRepository: UserRepository,
) {

	@Transactional
	fun send(requesterId: UUID, copyId: UUID, idempotencyKey: String?): RequestResponse {
		// The same tap sent twice (slow internet) gets the first request back instead of making a second one.
		val key = idempotencyKey?.trim()?.ifEmpty { null } ?: UUID.randomUUID().toString()
		requestRepository.findByRequesterIdAndIdempotencyKey(requesterId, key)?.let {
			return describe(listOf(it), requesterId).single()
		}

		val copy = copyRepository.findWithBookById(copyId)
		// A copy you can't see in any of your groups answers the same as a missing one.
		if (copy == null || copy.deletedAt != null ||
			(copy.ownerId != requesterId && !copyRepository.isVisibleTo(copyId, requesterId))
		) {
			throw ApiException(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Copy not found")
		}
		if (copy.ownerId == requesterId) {
			throw ApiException(HttpStatus.BAD_REQUEST, "OWN_COPY", "You can't borrow your own book")
		}
		checkCanBeLent(copy)
		if (requestRepository.existsByCopyIdAndRequesterIdAndStatus(copyId, requesterId, RequestStatus.PENDING)) {
			throw ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_SENT", "You already asked for this book")
		}

		val request = requestRepository.save(BorrowRequest(copy = copy, requesterId = requesterId, idempotencyKey = key))
		return describe(listOf(request), requesterId).single()
	}

	// "Waiting for an answer" – what I asked for.
	@Transactional(readOnly = true)
	fun sent(userId: UUID): List<RequestResponse> =
		describe(requestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, RequestStatus.PENDING), userId)

	// Requests for my books. A removed book's requests aren't shown.
	@Transactional(readOnly = true)
	fun received(userId: UUID): List<RequestResponse> =
		describe(
			requestRepository.findByCopyOwnerIdAndStatusOrderByCreatedAtDesc(userId, RequestStatus.PENDING)
				.filter { it.copy.deletedAt == null },
			userId,
		)

	// The book goes to this person for LOAN_DAYS. The other requests for it stay open.
	@Transactional
	fun accept(ownerId: UUID, requestId: UUID): RequestResponse {
		val request = pendingRequest(requestId) { it.copy.ownerId == ownerId }
		checkCanBeLent(request.copy)
		request.status = RequestStatus.ACCEPTED
		borrowingRepository.save(
			Borrowing(
				requestId = request.id!!,
				copyId = request.copy.id!!,
				borrowerId = request.requesterId,
				dueDate = Instant.now().plus(LOAN_DAYS),
			),
		)
		return describe(listOf(request), ownerId).single()
	}

	@Transactional
	fun reject(ownerId: UUID, requestId: UUID): RequestResponse {
		val request = pendingRequest(requestId) { it.copy.ownerId == ownerId }
		request.status = RequestStatus.REJECTED
		return describe(listOf(request), ownerId).single()
	}

	// The person who asked takes it back.
	@Transactional
	fun cancel(requesterId: UUID, requestId: UUID): RequestResponse {
		val request = pendingRequest(requestId) { it.requesterId == requesterId }
		request.status = RequestStatus.CANCELED
		return describe(listOf(request), requesterId).single()
	}

	// isAllowed says who may do this. Anyone else gets "not found", so nobody can poke at other people's requests.
	private fun pendingRequest(requestId: UUID, isAllowed: (BorrowRequest) -> Boolean): BorrowRequest {
		val request = requestRepository.findWithCopyById(requestId)
		if (request == null || !isAllowed(request)) {
			throw ApiException(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "Request not found")
		}
		if (request.status != RequestStatus.PENDING) {
			throw ApiException(HttpStatus.CONFLICT, "REQUEST_NOT_PENDING", "This request was already answered")
		}
		return request
	}

	private fun checkCanBeLent(copy: BookCopy) {
		if (copy.deletedAt != null || copy.availability != Availability.AVAILABLE) {
			throw ApiException(HttpStatus.CONFLICT, "COPY_NOT_AVAILABLE", "This book isn't available")
		}
		if (isLent(copy)) {
			throw ApiException(HttpStatus.CONFLICT, "COPY_LENT", "This book is lent out right now")
		}
	}

	private fun isLent(copy: BookCopy) =
		borrowingRepository.existsByCopyIdAndStatusNot(copy.id!!, BorrowingStatus.RETURNED)

	// Turns requests into answers for the app, with the people's names. viewerId = who's asking.
	private fun describe(requests: List<BorrowRequest>, viewerId: UUID): List<RequestResponse> {
		val names = userRepository.findAllById(requests.flatMap { listOf(it.requesterId, it.copy.ownerId) }.toSet())
			.associate { it.id!! to it.name }
		return requests.map { request ->
			val copy = request.copy
			RequestResponse(
				id = request.id!!,
				status = request.status,
				createdAt = request.createdAt,
				copyId = copy.id!!,
				book = RequestBookResponse(
					id = copy.book.id!!,
					isbn = copy.book.isbn,
					title = copy.book.title,
					authors = copy.book.authors,
					coverUrl = copy.book.coverUrl,
				),
				requester = PersonResponse(request.requesterId, names[request.requesterId].orEmpty()),
				owner = PersonResponse(copy.ownerId, names[copy.ownerId].orEmpty()),
				canAccept = viewerId == copy.ownerId &&
					request.status == RequestStatus.PENDING &&
					copy.deletedAt == null &&
					copy.availability == Availability.AVAILABLE &&
					!isLent(copy),
			)
		}
	}

	companion object {
		// The same as in the app (LoanDays).
		val LOAN_DAYS: Duration = Duration.ofDays(14)
	}
}
