package com.bookworms.server.reviews.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// My rating of a book. Sending it again changes the old one.
data class RateBookRequest(
	// Whole stars, 1–5.
	@field:Min(1)
	@field:Max(5)
	val rating: Int,
	// A few words about the book. Empty = only stars.
	@field:Size(max = MAX_CONTENT)
	val content: String? = null,
	val hasSpoilers: Boolean = false,
) {
	companion object {
		const val MAX_CONTENT = 2000
	}
}

// Everything about a book's ratings for Book details.
data class BookReviewsResponse(
	// In stars with one decimal, e.g. 4.5. null when nobody rated the book yet.
	val average: Double?,
	val count: Long,
	// The logged-in user's own stars, or null when they haven't rated it (or aren't logged in).
	val myRating: Int?,
	// The newest first.
	val reviews: List<ReviewResponse>,
)

data class ReviewResponse(
	val id: UUID,
	val authorName: String,
	val authorUsername: String,
	// A path on this server, e.g. "/photos/<file>.jpg", or null when the author has no photo.
	val authorPhotoUrl: String?,
	val rating: Int,
	val content: String?,
	val hasSpoilers: Boolean,
	val updatedAt: Instant,
)
