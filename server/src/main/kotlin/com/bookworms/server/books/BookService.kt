package com.bookworms.server.books

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
	private val bookRepository: BookRepository,
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
