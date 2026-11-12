package com.bookworms.server.books

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// A book in the public catalog – one row per book, however many copies people have.
@Entity
@Table(name = "books")
class Book(
	@field:Enumerated(EnumType.STRING)
	var source: BookSource,
	var title: String,
	var authors: String? = null,
	// Always ISBN-13 without dashes.
	var isbn: String? = null,
	var publisher: String? = null,
	var publishedYear: Short? = null,
	var pageCount: Int? = null,
	@field:Column(columnDefinition = "text")
	var description: String? = null,
	var coverUrl: String? = null,
	var googleBooksId: String? = null,
	var openLibraryId: String? = null,
	var createdBy: UUID? = null,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	val createdAt: Instant = Instant.now()

	@ManyToMany
	@JoinTable(
		name = "book_genres",
		joinColumns = [JoinColumn(name = "book_id")],
		inverseJoinColumns = [JoinColumn(name = "genre_id")],
	)
	var genres: MutableSet<Genre> = mutableSetOf()
}

enum class BookSource { GOOGLE_BOOKS, OPEN_LIBRARY, MANUAL }
