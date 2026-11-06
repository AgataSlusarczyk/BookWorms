package com.bookworms.server.books

import com.bookworms.server.books.dto.BookSummaryResponse
import com.bookworms.server.books.dto.toSummaryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// The public catalog – works without logging in (see SecurityConfig).
@RestController
@RequestMapping("/books")
class BookController(
	private val bookService: BookService,
) {

	@GetMapping
	fun search(
		@RequestParam(required = false) query: String?,
		@RequestParam(defaultValue = "50") limit: Int,
	): List<BookSummaryResponse> =
		bookService.search(query, limit).map { it.toSummaryResponse() }
}
