package com.bookworms.server.copies.dto

import com.bookworms.server.books.BookSource
import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopy
import java.time.Instant
import java.util.UUID

// One of my books, for My books and My copy.
data class CopyResponse(
	val id: UUID,
	val availability: Availability,
	val book: CopyBookResponse,
	// true = the owner typed this book in by hand, so the app can show Edit book.
	val canEditBook: Boolean,
	val createdAt: Instant,
	val updatedAt: Instant,
)

// The book the copy is of – what My books and the Edit book form show.
data class CopyBookResponse(
	val id: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val publisher: String?,
	val publishedYear: Int?,
	// A path on this server, e.g. "/covers/9788308075197.jpg", or null when there's no cover.
	val coverUrl: String?,
)

fun BookCopy.toResponse() = CopyResponse(
	id = id!!,
	availability = availability,
	book = CopyBookResponse(
		id = book.id!!,
		isbn = book.isbn,
		title = book.title,
		authors = book.authors,
		publisher = book.publisher,
		publishedYear = book.publishedYear?.toInt(),
		coverUrl = book.coverUrl,
	),
	canEditBook = book.source == BookSource.MANUAL && book.createdBy == ownerId,
	createdAt = createdAt,
	updatedAt = updatedAt,
)
