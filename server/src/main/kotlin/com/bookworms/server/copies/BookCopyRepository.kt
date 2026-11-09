package com.bookworms.server.copies

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BookCopyRepository : JpaRepository<BookCopy, UUID> {

	// My books, newest first. Removed ones are skipped. Loads the book too, because the list shows its title.
	@EntityGraph(attributePaths = ["book"])
	fun findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId: UUID): List<BookCopy>

	// One copy with its book, because the answer after a change shows the title too.
	@EntityGraph(attributePaths = ["book"])
	fun findWithBookById(id: UUID): BookCopy?
}
