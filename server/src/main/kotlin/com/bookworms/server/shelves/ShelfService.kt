package com.bookworms.server.shelves

import com.bookworms.server.books.BookNotFoundException
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.dto.toSummaryResponse
import com.bookworms.server.lists.BookList
import com.bookworms.server.lists.BookListItem
import com.bookworms.server.lists.BookListItemId
import com.bookworms.server.lists.BookListItemRepository
import com.bookworms.server.lists.BookListRepository
import com.bookworms.server.lists.ListType
import com.bookworms.server.shelves.dto.MyShelfStateResponse
import com.bookworms.server.shelves.dto.ShelfBookResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// The user's shelves on My lists: the books they marked with a heart (Favorites) or a reading status
// (Want to read, Reading, Read). Only the user sees them.
@Service
class ShelfService(
	private val statusRepository: UserBookStatusRepository,
	private val listRepository: BookListRepository,
	private val itemRepository: BookListItemRepository,
	private val bookRepository: BookRepository,
) {

	// Every book with a heart or a status – each once, with both. The app sorts them onto the shelves.
	@Transactional(readOnly = true)
	fun shelf(userId: UUID): List<ShelfBookResponse> {
		val statuses = statusRepository.findById_UserIdAndDeletedAtIsNull(userId).associateBy { it.id.bookId }
		val favorites = favoritesList(userId)?.let { itemRepository.findById_ListId(it.id!!) }.orEmpty()
			.associateBy { it.id.bookId }
		return bookRepository.findAllById(statuses.keys + favorites.keys).map { book ->
			val status = statuses[book.id]
			val favorite = favorites[book.id]
			ShelfBookResponse(
				book = book.toSummaryResponse(),
				status = status?.status,
				statusChangedAt = status?.updatedAt,
				favorite = favorite != null,
				favoritedAt = favorite?.addedAt,
			)
		}
	}

	// The heart and the status on Book details.
	@Transactional(readOnly = true)
	fun book(userId: UUID, bookId: UUID): MyShelfStateResponse {
		requireBook(bookId)
		return MyShelfStateResponse(status = currentStatus(userId, bookId)?.status, favorite = isFavorite(userId, bookId))
	}

	@Transactional
	fun setStatus(userId: UUID, bookId: UUID, status: ReadingStatus): MyShelfStateResponse {
		requireBook(bookId)
		val row = statusRepository.findByIdOrNull(UserBookId(userId, bookId))
		when {
			row == null -> statusRepository.save(UserBookStatus(UserBookId(userId, bookId), status))
			// The same status again changes nothing, so "added 5 days ago" stays right.
			row.deletedAt == null && row.status == status -> Unit
			else -> {
				row.status = status
				row.deletedAt = null
				row.updatedAt = Instant.now()
			}
		}
		return MyShelfStateResponse(status = status, favorite = isFavorite(userId, bookId))
	}

	// Nothing to take back is fine too.
	@Transactional
	fun removeStatus(userId: UUID, bookId: UUID): MyShelfStateResponse {
		requireBook(bookId)
		currentStatus(userId, bookId)?.let {
			it.deletedAt = Instant.now()
			it.updatedAt = Instant.now()
		}
		return MyShelfStateResponse(status = null, favorite = isFavorite(userId, bookId))
	}

	// Tapping the heart twice is fine – the book is on Favorites once.
	@Transactional
	fun addFavorite(userId: UUID, bookId: UUID): MyShelfStateResponse {
		requireBook(bookId)
		// Made the first time it's needed.
		val list = favoritesList(userId) ?: listRepository.save(BookList(userId, FAVORITES_NAME, ListType.FAVORITES))
		val itemId = BookListItemId(list.id!!, bookId)
		if (!itemRepository.existsById(itemId)) {
			itemRepository.save(BookListItem(itemId, position = itemRepository.lastPosition(list.id!!) + 1))
			list.updatedAt = Instant.now()
		}
		return MyShelfStateResponse(status = currentStatus(userId, bookId)?.status, favorite = true)
	}

	@Transactional
	fun removeFavorite(userId: UUID, bookId: UUID): MyShelfStateResponse {
		requireBook(bookId)
		favoritesList(userId)?.let { list ->
			itemRepository.findByIdOrNull(BookListItemId(list.id!!, bookId))?.let {
				itemRepository.delete(it)
				list.updatedAt = Instant.now()
			}
		}
		return MyShelfStateResponse(status = currentStatus(userId, bookId)?.status, favorite = false)
	}

	private fun favoritesList(userId: UUID): BookList? = listRepository.findByOwnerIdAndType(userId, ListType.FAVORITES)

	private fun isFavorite(userId: UUID, bookId: UUID): Boolean =
		favoritesList(userId)?.let { itemRepository.existsById(BookListItemId(it.id!!, bookId)) } ?: false

	private fun currentStatus(userId: UUID, bookId: UUID): UserBookStatus? =
		statusRepository.findByIdOrNull(UserBookId(userId, bookId))?.takeIf { it.deletedAt == null }

	private fun requireBook(bookId: UUID) {
		if (!bookRepository.existsById(bookId)) throw BookNotFoundException()
	}

	private companion object {
		// The app shows its own, translated name – this one is only in the database.
		const val FAVORITES_NAME = "Favorites"
	}
}
