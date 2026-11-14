package com.bookworms.server.books

import com.bookworms.server.common.ApiException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
	private val bookRepository: BookRepository,
	private val coverDownloader: CoverDownloader,
) {

	// No query = the newest books. Otherwise books whose title, author or ISBN contains it.
	// Letter case doesn't matter, and dashes or spaces in an ISBN are skipped, so "978-83-08" works too.
	@Transactional(readOnly = true)
	fun search(query: String?, limit: Int): List<Book> {
		val page = PageRequest.of(0, limit.coerceIn(1, MAX_LIMIT))
		val text = query?.trim().orEmpty()
		if (text.isEmpty()) {
			return bookRepository.findNewest(page)
		}
		// Only digits, dashes and spaces means the user typed (part of) an ISBN.
		val looksLikeIsbn = text.any { it.isDigit() } && text.all { it.isDigit() || it == '-' || it == ' ' }
		val isbnDigits = if (looksLikeIsbn) text.filter { it.isDigit() } else ""
		return bookRepository.search(likePattern(text), isbnDigits, page)
	}

	// The ISBN may have dashes or spaces, e.g. "978-83-08-04946-1".
	// Books not in the catalog yet are looked up in Open Library / Google Books and saved.
	@Transactional
	fun findByIsbn(isbn: String): Book {
		val cleanIsbn = isbn.uppercase().filter { it.isDigit() || it == 'X' }
		bookRepository.findByIsbn(cleanIsbn)?.let { return it }

		val fetched = coverDownloader.fetchBook(cleanIsbn) ?: throw BookNotFoundException()
		fetched.coverUrl = coverDownloader.download(cleanIsbn)
		return bookRepository.save(fetched)
	}

	// "%text%" in lower case, with % and _ typed by the user treated as plain characters.
	private fun likePattern(text: String): String {
		val escaped = text.lowercase()
			.replace("!", "!!")
			.replace("%", "!%")
			.replace("_", "!_")
		return "%$escaped%"
	}

	companion object {
		const val MAX_LIMIT = 100
	}
}

class BookNotFoundException :
	ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found")