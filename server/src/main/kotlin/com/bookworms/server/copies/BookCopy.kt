package com.bookworms.server.copies

import com.bookworms.server.books.Book
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

// One physical book someone owns – "Agata has Solaris". The book itself (title, author) is in the catalog.
@Entity
@Table(name = "book_copies")
class BookCopy(
	@field:ManyToOne(fetch = FetchType.LAZY, optional = false)
	@field:JoinColumn(name = "book_id")
	var book: Book,
	var ownerId: UUID,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// Set only by the owner. "Lent out" isn't kept here – it comes from the loans.
	@Enumerated(EnumType.STRING)
	var availability: Availability = Availability.AVAILABLE

	// Goes up on every save. If two changes clash, the second one fails instead of overwriting the first.
	@Version
	var version: Int = 0

	val createdAt: Instant = Instant.now()

	// The phone will ask for "what changed since…", so every change moves this.
	var updatedAt: Instant = Instant.now()

	// Removing a copy only sets this. The row stays, so phones can learn it's gone.
	var deletedAt: Instant? = null

	@PreUpdate
	fun touch() {
		updatedAt = Instant.now()
	}
}

enum class Availability { AVAILABLE, UNAVAILABLE, LOST }
