package com.bookworms.server.shelves

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable
import java.time.Instant
import java.util.UUID

// Where the user is with a book: going to read it, reading it, done. It's about the book from the catalog,
// not a copy, so it works for borrowed books and books the user doesn't have too.
enum class ReadingStatus { TO_READ, READING, READ, ABANDONED }

// The user's reading status of one book. Taking it back sets deletedAt – the row stays,
// so phones can later learn it's gone (see the schema, part C).
@Entity
@Table(name = "user_book_status")
class UserBookStatus(
	@field:EmbeddedId
	val id: UserBookId,
	@field:Enumerated(EnumType.STRING)
	var status: ReadingStatus,
) {
	// When the status last changed – "added 5 days ago" on the shelf.
	var updatedAt: Instant = Instant.now()

	var deletedAt: Instant? = null
}

@Embeddable
data class UserBookId(
	val userId: UUID,
	val bookId: UUID,
) : Serializable

interface UserBookStatusRepository : JpaRepository<UserBookStatus, UserBookId> {

	// "Id_UserId" means id.userId – the user part of the key. Taken-back statuses are skipped.
	fun findById_UserIdAndDeletedAtIsNull(userId: UUID): List<UserBookStatus>
}
