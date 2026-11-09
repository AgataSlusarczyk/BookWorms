package com.bookworms.server.copies

import com.bookworms.server.books.Book
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.BookSource
import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.dto.AddCopyRequest
import com.bookworms.server.copies.dto.EditBookRequest
import com.bookworms.server.loans.BorrowRequestRepository
import com.bookworms.server.loans.BorrowingRepository
import com.bookworms.server.loans.BorrowingStatus
import com.bookworms.server.loans.QueueRepository
import com.bookworms.server.loans.RequestStatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// The user's own books: add, list, change, remove. Only the owner can touch a copy.
@Service
class CopyService(
	private val copyRepository: BookCopyRepository,
	private val bookRepository: BookRepository,
	private val borrowingRepository: BorrowingRepository,
	private val requestRepository: BorrowRequestRepository,
	private val queueRepository: QueueRepository,
) {

	@Transactional(readOnly = true)
	fun myCopies(ownerId: UUID): List<BookCopy> =
		copyRepository.findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId)

	// A book with this ISBN already in the catalog gets the new copy, so copies of one book stay together.
	// Otherwise (or without an ISBN) the book is added to the catalog first, as typed in by this user.
	@Transactional
	fun addCopy(ownerId: UUID, request: AddCopyRequest): BookCopy {
		val isbn = request.isbn?.let { toIsbn13(it) }
		val book = isbn?.let { bookRepository.findByIsbn(it) }
			?: bookRepository.save(
				Book(
					source = BookSource.MANUAL,
					title = request.title.trim(),
					authors = request.authors.trim(),
					isbn = isbn,
					publisher = request.publisher?.trim()?.ifEmpty { null },
					publishedYear = request.publishedYear?.toShort(),
					createdBy = ownerId,
				),
			)
		// Flushed now, because the next query is plain SQL and needs the copy's row to be in the database already.
		val copy = copyRepository.saveAndFlush(BookCopy(book = book, ownerId = ownerId))
		copyRepository.showInAllOwnersGroups(copy.id!!, ownerId)
		return copy
	}

	@Transactional
	fun changeAvailability(ownerId: UUID, copyId: UUID, availability: Availability): BookCopy {
		val copy = myCopy(ownerId, copyId)
		copy.availability = availability
		if (availability == Availability.AVAILABLE &&
			!borrowingRepository.existsByCopyIdAndStatusNot(copyId, BorrowingStatus.RETURNED)
		) {
			// TODO: tell the people waiting once notifications are on the server.
			queueRepository.deleteById_CopyId(copyId)
		}
		return copy
	}

	// Only a book this user typed in by hand – a catalog book is shared, so changing it would change it for everyone.
	@Transactional
	fun editBook(ownerId: UUID, copyId: UUID, request: EditBookRequest): BookCopy {
		val copy = myCopy(ownerId, copyId)
		val book = copy.book
		if (book.source != BookSource.MANUAL || book.createdBy != ownerId) {
			throw ApiException(HttpStatus.FORBIDDEN, "BOOK_NOT_EDITABLE", "Only books you added by hand can be edited")
		}
		book.title = request.title.trim()
		book.authors = request.authors.trim()
		book.publisher = request.publisher?.trim()?.ifEmpty { null }
		book.publishedYear = request.publishedYear?.toShort()
		// The book's row isn't the copy's, so the copy's updatedAt is moved by hand – phones need to see the change.
		copy.updatedAt = Instant.now()
		return copy
	}

	// The row stays with deletedAt set, so phones can learn the book is gone.
	// A lent-out book can't be removed – someone has it, and it has to come back first.
	// Requests still waiting for it are rejected, so the people who asked aren't left waiting forever.
	@Transactional
	fun removeCopy(ownerId: UUID, copyId: UUID) {
		val copy = myCopy(ownerId, copyId)
		if (borrowingRepository.existsByCopyIdAndStatusNot(copyId, BorrowingStatus.RETURNED)) {
			throw ApiException(HttpStatus.CONFLICT, "COPY_LENT", "This book is lent out right now")
		}
		copy.deletedAt = Instant.now()
		requestRepository.findByCopyIdAndStatus(copyId, RequestStatus.PENDING)
			.forEach { it.status = RequestStatus.REJECTED }
		queueRepository.deleteById_CopyId(copyId)
	}

	// Someone else's copy that this user can see – it's in one of their groups. Used for asking and for the queue.
	// A copy they can't see answers the same as a missing one.
	@Transactional(readOnly = true)
	fun othersCopy(userId: UUID, copyId: UUID): BookCopy {
		val copy = copyRepository.findWithBookById(copyId)
		if (copy == null || copy.deletedAt != null ||
			(copy.ownerId != userId && !copyRepository.isVisibleTo(copyId, userId))
		) {
			throw ApiException(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Copy not found")
		}
		if (copy.ownerId == userId) {
			throw ApiException(HttpStatus.BAD_REQUEST, "OWN_COPY", "This is your own book")
		}
		return copy
	}

	// Someone else's copy answers the same as a missing one, so nobody can guess other people's copies.
	private fun myCopy(ownerId: UUID, copyId: UUID): BookCopy {
		val copy = copyRepository.findWithBookById(copyId)
		if (copy == null || copy.deletedAt != null || copy.ownerId != ownerId) {
			throw ApiException(HttpStatus.NOT_FOUND, "COPY_NOT_FOUND", "Copy not found")
		}
		return copy
	}
}

// The catalog keeps ISBN-13 without dashes. An old ISBN-10 is turned into its ISBN-13 (978 in front, new check digit).
// Empty = no ISBN. Anything else that isn't an ISBN is an error.
internal fun toIsbn13(text: String): String? {
	val isbn = text.filter { it.isDigit() || it == 'X' || it == 'x' }.uppercase()
	return when {
		isbn.isEmpty() -> null
		isbn.length == 13 && isbn.all { it.isDigit() } -> isbn
		isbn.length == 10 && isbn.take(9).all { it.isDigit() } -> {
			val first12 = "978" + isbn.take(9)
			val sum = first12.mapIndexed { i, c -> (c - '0') * if (i % 2 == 0) 1 else 3 }.sum()
			first12 + (10 - sum % 10) % 10
		}
		else -> throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_ISBN", "ISBN must have 10 or 13 digits")
	}
}
