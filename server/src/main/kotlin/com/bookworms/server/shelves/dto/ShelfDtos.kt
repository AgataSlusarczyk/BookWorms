package com.bookworms.server.shelves.dto

import com.bookworms.server.books.dto.BookSummaryResponse
import com.bookworms.server.shelves.ReadingStatus
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class SetReadingStatusRequest(
	@field:NotNull
	val status: ReadingStatus?,
)

// The heart and the reading status of one book, for Book details.
data class MyShelfStateResponse(
	// null = the user didn't pick one.
	val status: ReadingStatus?,
	val favorite: Boolean,
)

// One book on the user's shelves.
data class ShelfBookResponse(
	val book: BookSummaryResponse,
	val status: ReadingStatus?,
	// When the status was last changed. null without a status.
	val statusChangedAt: Instant?,
	val favorite: Boolean,
	// When the heart was tapped. null without one.
	val favoritedAt: Instant?,
)
