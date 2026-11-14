package com.bookworms.server.reviews

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BookReviewRepository : JpaRepository<BookReview, UUID> {

	fun findByBookIdAndUserId(bookId: UUID, userId: UUID): BookReview?

	// The newest first. Older ones aren't sent – nobody scrolls that far.
	fun findTop100ByBookIdAndIsHiddenFalseOrderByUpdatedAtDesc(bookId: UUID): List<BookReview>

	fun countByBookIdAndIsHiddenFalse(bookId: UUID): Long

	// Stars × 2, like in the table. null when nobody rated the book.
	@Query("select avg(r.rating) from BookReview r where r.bookId = :bookId and r.isHidden = false")
	fun averageRating(bookId: UUID): Double?
}
