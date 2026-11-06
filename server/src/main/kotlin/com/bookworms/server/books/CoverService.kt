package com.bookworms.server.books

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CoverService(
	private val bookRepository: BookRepository,
	private val coverDownloader: CoverDownloader,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	// Gives a cover to every book that has an ISBN but no cover yet. Slow (it asks other servers),
	// so call it in the background. A book whose cover isn't found stays without one.
	fun fetchMissingCovers() {
		val books = bookRepository.findByIsbnIsNotNullAndCoverUrlIsNull()
		var found = 0
		for (book in books) {
			val coverUrl = coverDownloader.download(book.isbn!!) ?: continue
			book.coverUrl = coverUrl
			bookRepository.save(book)
			found++
		}
		if (books.isNotEmpty()) {
			log.info("Covers: found {} of {} missing", found, books.size)
		}
	}
}
