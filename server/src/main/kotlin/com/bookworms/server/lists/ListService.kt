package com.bookworms.server.lists

import com.bookworms.server.books.BookNotFoundException
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.dto.toSummaryResponse
import com.bookworms.server.common.ApiException
import com.bookworms.server.lists.dto.ListBookResponse
import com.bookworms.server.lists.dto.ListDetailsResponse
import com.bookworms.server.lists.dto.ListSummaryResponse
import com.bookworms.server.loans.BookAvailabilityService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// The lists the user makes themselves on My lists ("Custom lists"). Only the user sees them.
// Favorites is a list too, but it has its own tab and is changed with the heart – see ShelfService.
@Service
class ListService(
	private val listRepository: BookListRepository,
	private val itemRepository: BookListItemRepository,
	private val bookRepository: BookRepository,
	private val availabilityService: BookAvailabilityService,
) {

	// With bookId: also says which lists have that book – for "Add to list" on Book details.
	@Transactional(readOnly = true)
	fun lists(userId: UUID, bookId: UUID? = null): List<ListSummaryResponse> {
		val lists = listRepository.findByOwnerIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(userId, ListType.CUSTOM)
		val items = itemRepository.findById_ListIdIn(lists.map { it.id!! })
		val counts = items.groupingBy { it.id.listId }.eachCount()
		val withBook = items.filter { it.id.bookId == bookId }.map { it.id.listId }.toSet()
		return lists.map { list ->
			list.toSummary(counts[list.id] ?: 0).copy(containsBook = bookId?.let { list.id in withBook })
		}
	}

	@Transactional(readOnly = true)
	fun list(userId: UUID, listId: UUID): ListDetailsResponse {
		val list = ownList(userId, listId)
		val items = itemRepository.findById_ListId(listId).associateBy { it.id.bookId }
		val availability = availabilityService.forBooks(userId, items.keys)
		val books = bookRepository.findAllById(items.keys)
			.map { book ->
				ListBookResponse(
					book = book.toSummaryResponse(),
					addedAt = items.getValue(book.id!!).addedAt,
					availability = availability[book.id],
				)
			}
			.sortedByDescending { it.addedAt }
		return ListDetailsResponse(id = list.id!!, name = list.name, description = list.description, books = books)
	}

	@Transactional
	fun create(userId: UUID, name: String, description: String?): ListSummaryResponse {
		val trimmedName = name.trim()
		requireFreeName(userId, trimmedName)
		val list = BookList(userId, trimmedName)
		list.description = cleanDescription(description)
		return listRepository.save(list).toSummary(bookCount = 0)
	}

	// Edit list: a new name and description. The list may keep its own name, also with other big/small letters.
	@Transactional
	fun update(userId: UUID, listId: UUID, name: String, description: String?): ListSummaryResponse {
		val list = ownList(userId, listId)
		val trimmedName = name.trim()
		if (!trimmedName.equals(list.name, ignoreCase = true)) requireFreeName(userId, trimmedName)
		list.name = trimmedName
		list.description = cleanDescription(description)
		list.updatedAt = Instant.now()
		return list.toSummary(bookCount = itemRepository.findById_ListId(listId).size)
	}

	// A book goes on the list once – adding it again changes nothing.
	@Transactional
	fun addBook(userId: UUID, listId: UUID, bookId: UUID) {
		val list = ownList(userId, listId)
		requireBook(bookId)
		val itemId = BookListItemId(listId, bookId)
		if (itemRepository.existsById(itemId)) return
		itemRepository.save(BookListItem(itemId, position = itemRepository.lastPosition(listId) + 1))
		list.updatedAt = Instant.now()
	}

	// A book that isn't on the list is fine too.
	@Transactional
	fun removeBook(userId: UUID, listId: UUID, bookId: UUID) {
		val list = ownList(userId, listId)
		requireBook(bookId)
		itemRepository.findByIdOrNull(BookListItemId(listId, bookId))?.let {
			itemRepository.delete(it)
			list.updatedAt = Instant.now()
		}
	}

	// The list only gets marked as deleted, so phones that still have it can tell it's gone.
	// Its name is free again right away.
	@Transactional
	fun delete(userId: UUID, listId: UUID) {
		val list = ownList(userId, listId)
		list.deletedAt = Instant.now()
		list.updatedAt = Instant.now()
	}

	// Someone else's list looks the same as one that doesn't exist, so nobody can check what ids others have.
	private fun ownList(userId: UUID, listId: UUID): BookList =
		listRepository.findByIdOrNull(listId)
			?.takeIf { it.ownerId == userId && it.type == ListType.CUSTOM && it.deletedAt == null }
			?: throw ListNotFoundException()

	private fun requireBook(bookId: UUID) {
		if (!bookRepository.existsById(bookId)) throw BookNotFoundException()
	}

	private fun requireFreeName(userId: UUID, name: String) {
		if (listRepository.existsByOwnerIdAndDeletedAtIsNullAndNameIgnoreCase(userId, name)) throw ListNameTakenException()
	}

	// An empty description is the same as none.
	private fun cleanDescription(description: String?): String? = description?.trim()?.ifEmpty { null }

	private fun BookList.toSummary(bookCount: Int) = ListSummaryResponse(
		id = id!!,
		name = name,
		description = description,
		bookCount = bookCount,
		createdAt = createdAt,
	)
}

class ListNameTakenException :
	ApiException(HttpStatus.CONFLICT, "LIST_NAME_TAKEN", "You already have a list with this name")

class ListNotFoundException :
	ApiException(HttpStatus.NOT_FOUND, "LIST_NOT_FOUND", "List not found")
