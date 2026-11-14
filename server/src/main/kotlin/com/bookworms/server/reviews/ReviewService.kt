package com.bookworms.server.reviews

import com.bookworms.server.books.BookNotFoundException
import com.bookworms.server.books.BookRepository
import com.bookworms.server.reviews.dto.BookReviewsResponse
import com.bookworms.server.reviews.dto.RateBookRequest
import com.bookworms.server.reviews.dto.ReviewResponse
import com.bookworms.server.users.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

// Ratings of books. Everyone reads them; only a logged-in user rates, once per book.
@Service
class ReviewService(
	private val reviewRepository: BookReviewRepository,
	private val bookRepository: BookRepository,
	private val userRepository: UserRepository,
) {

	// userId is null for someone browsing without an account.
	@Transactional(readOnly = true)
	fun reviews(bookId: UUID, userId: UUID?): BookReviewsResponse {
		requireBook(bookId)
		val reviews = reviewRepository.findTop100ByBookIdAndIsHiddenFalseOrderByUpdatedAtDesc(bookId)
		// One query for all the authors' names.
		val authors = userRepository.findAllById(reviews.map { it.userId }.toSet()).associateBy { it.id }
		return BookReviewsResponse(
			// The table keeps stars × 2, so the average is halved and rounded to one decimal.
			average = reviewRepository.averageRating(bookId)?.let { (it / 2 * 10).roundToInt() / 10.0 },
			count = reviewRepository.countByBookIdAndIsHiddenFalse(bookId),
			myRating = userId?.let { reviewRepository.findByBookIdAndUserId(bookId, it) }
				?.takeIf { !it.isHidden }
				?.let { it.rating / 2 },
			reviews = reviews.mapNotNull { review ->
				// A deleted account can't be here (users can't be deleted while they have reviews), but just in case.
				val author = authors[review.userId] ?: return@mapNotNull null
				ReviewResponse(
					id = review.id!!,
					authorName = author.name,
					authorUsername = author.username,
					rating = review.rating / 2,
					content = review.content,
					hasSpoilers = review.hasSpoilers,
					updatedAt = review.updatedAt,
				)
			},
		)
	}

	// The first rating adds a review; the next ones change it.
	@Transactional
	fun rate(bookId: UUID, userId: UUID, request: RateBookRequest): BookReviewsResponse {
		requireBook(bookId)
		val content = request.content?.trim()?.ifEmpty { null }
		val review = reviewRepository.findByBookIdAndUserId(bookId, userId)
		if (review == null) {
			reviewRepository.save(
				BookReview(
					bookId = bookId,
					userId = userId,
					rating = (request.rating * 2).toShort(),
					content = content,
					hasSpoilers = request.hasSpoilers && content != null,
				),
			)
		} else {
			review.rating = (request.rating * 2).toShort()
			review.content = content
			review.hasSpoilers = request.hasSpoilers && content != null
			review.updatedAt = Instant.now()
		}
		// Saved now, so the counts below already include it.
		reviewRepository.flush()
		return reviews(bookId, userId)
	}

	// Taking a rating back. Nothing to take back is fine too.
	@Transactional
	fun removeRating(bookId: UUID, userId: UUID): BookReviewsResponse {
		requireBook(bookId)
		reviewRepository.findByBookIdAndUserId(bookId, userId)?.let {
			reviewRepository.delete(it)
			reviewRepository.flush()
		}
		return reviews(bookId, userId)
	}

	private fun requireBook(bookId: UUID) {
		if (!bookRepository.existsById(bookId)) throw BookNotFoundException()
	}
}
