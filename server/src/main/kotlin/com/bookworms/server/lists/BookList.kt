package com.bookworms.server.lists

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.io.Serializable
import java.time.Instant
import java.util.UUID

// FAVORITES = the heart on Book details. Each user has at most one, made the first time they tap the heart.
enum class ListType { CUSTOM, FAVORITES }

// GROUPS = people in the chosen groups can see the list. Not used yet – every list is PRIVATE.
enum class ListVisibility { PRIVATE, GROUPS }

// One of the user's lists of books.
@Entity
@Table(name = "book_lists")
class BookList(
	val ownerId: UUID,
	var name: String,
	@field:Enumerated(EnumType.STRING)
	val type: ListType = ListType.CUSTOM,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	@field:Column(columnDefinition = "text")
	var description: String? = null

	@Enumerated(EnumType.STRING)
	var visibility: ListVisibility = ListVisibility.PRIVATE

	val createdAt: Instant = Instant.now()

	// Changes with every book added or taken off too, so phones can tell the list changed.
	var updatedAt: Instant = Instant.now()

	var deletedAt: Instant? = null
}

// One book on a list.
@Entity
@Table(name = "book_list_items")
class BookListItem(
	@field:EmbeddedId
	val id: BookListItemId,
	// The order on the list – a new book goes to the end.
	var position: Int,
) {
	// e.g. "recommended by Kasia". Not used yet.
	@field:Column(columnDefinition = "text")
	var note: String? = null

	val addedAt: Instant = Instant.now()
}

@Embeddable
data class BookListItemId(
	val listId: UUID,
	val bookId: UUID,
) : Serializable

interface BookListRepository : JpaRepository<BookList, UUID> {

	fun findByOwnerIdAndType(ownerId: UUID, type: ListType): BookList?

	// The user's own lists (not Favorites), newest first. Deleted ones are left out.
	fun findByOwnerIdAndTypeAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId: UUID, type: ListType): List<BookList>

	// Big and small letters count as the same. Favorites counts too – the database doesn't allow that name twice either.
	fun existsByOwnerIdAndDeletedAtIsNullAndNameIgnoreCase(ownerId: UUID, name: String): Boolean
}

interface BookListItemRepository : JpaRepository<BookListItem, BookListItemId> {

	// "Id_ListId" means id.listId – the list part of the key.
	fun findById_ListId(listId: UUID): List<BookListItem>

	// The books on several lists at once.
	fun findById_ListIdIn(listIds: Collection<UUID>): List<BookListItem>

	// 0 for an empty list.
	@Query("select coalesce(max(i.position), 0) from BookListItem i where i.id.listId = :listId")
	fun lastPosition(listId: UUID): Int
}
