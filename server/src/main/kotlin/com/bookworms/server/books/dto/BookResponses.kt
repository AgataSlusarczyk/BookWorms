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

// Everything about one book for the details screen.
data class BookDetailsResponse(
	val id: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val publisher: String?,
	val publishedYear: Int?,
	val pageCount: Int?,
	val description: String?,
	val coverUrl: String?,
	// Genre codes, e.g. ["SCI_FI"]. The app turns them into names.
	val genres: List<String>,
)

fun Book.toSummaryResponse() = BookSummaryResponse(
	id = id!!,
	isbn = isbn,
	title = title,
	authors = authors,
	publishedYear = publishedYear?.toInt(),
)

fun Book.toDetailsResponse() = BookDetailsResponse(
	id = id!!,
	isbn = isbn,
	title = title,
	authors = authors,
	publisher = publisher,
	publishedYear = publishedYear?.toInt(),
	pageCount = pageCount,
	description = description,
	coverUrl = coverUrl,
	genres = genres.map { it.code }.sorted(),
)
