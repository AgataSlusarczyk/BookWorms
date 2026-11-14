package com.bookworms.server.reviews

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// One person's rating of one book, maybe with a few words. Everyone can read it, even without an account.
@Entity
@Table(name = "book_reviews")
class BookReview(
	val bookId: UUID,
	val userId: UUID,
	// Stars × 2 (2 = ★1 … 10 = ★5), so half stars can be added later without changing the table.
	var rating: Short,
	@field:Column(columnDefinition = "text")
	var content: String? = null,
	var hasSpoilers: Boolean = false,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	// An admin can hide a review that breaks the rules. Hidden ones don't count anywhere.
	var isHidden: Boolean = false

	val createdAt: Instant = Instant.now()

	var updatedAt: Instant = Instant.now()
}
