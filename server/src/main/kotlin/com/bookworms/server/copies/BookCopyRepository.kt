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

	@EntityGraph(attributePaths = ["book"])
	fun findWithBookByIdIn(ids: Collection<UUID>): List<BookCopy>

	// TODO: use the group entities instead of plain SQL once groups are on the server.
	// A new book starts visible in all the owner's groups. Choosing the groups comes with the groups on the server.
	@Modifying
	@Query(
		value = """
		insert into copy_group_visibility (copy_id, group_id)
		select :copyId, group_id from group_members where user_id = :ownerId
		""",
		nativeQuery = true,
	)
	fun showInAllOwnersGroups(copyId: UUID, ownerId: UUID)

	// TODO: use the group entities instead of plain SQL once groups are on the server.
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
