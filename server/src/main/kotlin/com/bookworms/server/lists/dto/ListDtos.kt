package com.bookworms.server.lists.dto

import com.bookworms.server.books.dto.BookSummaryResponse
import com.bookworms.server.loans.BookAvailability
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// A new list, or a list after Edit list: a name, and optionally a few words about it.
data class ListRequest(
	// The same limit as the field in the app. Spaces around it are cut off.
	@field:NotBlank
	@field:Size(max = 30)
	val name: String,

	@field:Size(max = 500)
	val description: String? = null,
)

// One of the user's own lists, for the tiles on My lists.
data class ListSummaryResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val bookCount: Int,
	val createdAt: Instant,
	// Only when asked about one book (GET /me/lists?bookId=…): whether it's on this list. Otherwise null.
	val containsBook: Boolean? = null,
)

// One list opened, with its books – the newest first.
data class ListDetailsResponse(
	val id: UUID,
	val name: String,
	val description: String?,
	val books: List<ListBookResponse>,
)

// One book on a list, and when it was put there.
data class ListBookResponse(
	val book: BookSummaryResponse,
	val addedAt: Instant,
	// Whether someone in the user's groups can lend it. null = nobody there has it to lend.
	val availability: BookAvailability?,
)
