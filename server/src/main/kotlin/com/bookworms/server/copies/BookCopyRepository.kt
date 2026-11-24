package com.bookworms.server.copies

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BookCopyRepository : JpaRepository<BookCopy, UUID> {

	// My books, newest first. Removed ones are skipped. Loads the book too, because the list shows its title.
	@EntityGraph(attributePaths = ["book"])
	fun findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId: UUID): List<BookCopy>

	// One copy with its book, because the answer after a change shows the title too.
	@EntityGraph(attributePaths = ["book"])
	fun findWithBookById(id: UUID): BookCopy?

	// Does this user already have the book? Removed copies don't count.
	fun existsByOwnerIdAndBook_IsbnAndDeletedAtIsNull(ownerId: UUID, isbn: String): Boolean

	// The same for a book without an ISBN: one with the same title and author, whatever the letter case.
	fun existsByOwnerIdAndBook_IsbnIsNullAndBook_TitleIgnoreCaseAndBook_AuthorsIgnoreCaseAndDeletedAtIsNull(
		ownerId: UUID,
		title: String,
		authors: String,
	): Boolean

	@EntityGraph(attributePaths = ["book"])
	fun findWithBookByIdIn(ids: Collection<UUID>): List<BookCopy>

	// Other people's copies of one book, the oldest first. Removed ones are skipped.
	fun findByBook_IdAndOwnerIdNotAndDeletedAtIsNullOrderByCreatedAtAsc(bookId: UUID, ownerId: UUID): List<BookCopy>

	// Other people's copies of several books at once. Removed ones are skipped.
	fun findByBook_IdInAndOwnerIdNotAndDeletedAtIsNull(bookIds: Collection<UUID>, ownerId: UUID): List<BookCopy>

	// The copies of a group's library, newest first, with their books. Removed ones are skipped.
	@EntityGraph(attributePaths = ["book"])
	fun findByIdInAndDeletedAtIsNullOrderByCreatedAtDesc(ids: Collection<UUID>): List<BookCopy>

	// A new book starts visible in all the owner's groups. My copy can change that (PUT /copies/{id}/groups).
	@Modifying
	@Query(
		value = """
		insert into copy_group_visibility (copy_id, group_id)
		select :copyId, group_id from group_members where user_id = :ownerId
		""",
		nativeQuery = true,
	)
	fun showInAllOwnersGroups(copyId: UUID, ownerId: UUID)

	// Is the copy visible in at least one group this user is in? Only then they can see it and ask to borrow it.
	@Query(
		value = """
		select exists (
			select 1 from copy_group_visibility v
			join group_members m on m.group_id = v.group_id
			where v.copy_id = :copyId and m.user_id = :userId
		)
		""",
		nativeQuery = true,
	)
	fun isVisibleTo(copyId: UUID, userId: UUID): Boolean
}
