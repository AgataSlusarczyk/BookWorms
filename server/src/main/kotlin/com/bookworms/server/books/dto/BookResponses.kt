package com.bookworms.server.books.dto

import com.bookworms.server.books.Book
import java.util.UUID

// One book in a list or in search results – only what the card shows.
data class BookSummaryResponse(
	val id: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val publishedYear: Int?,
)

fun Book.toSummaryResponse() = BookSummaryResponse(
	id = id!!,
	isbn = isbn,
	title = title,
	authors = authors,
	publishedYear = publishedYear?.toInt(),
)
