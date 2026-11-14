package com.bookworms.server.books

import com.bookworms.server.books.dto.BookSummaryResponse
import com.bookworms.server.books.dto.toSummaryResponse
import com.bookworms.server.common.userId
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// The Library tab: "Recently added" in the user's groups. Needs a token (see SecurityConfig).
@RestController
class LibraryController(
	private val bookService: BookService,
) {

	// Books the user has, or someone has in one of their groups – each book once, the newest copy first.
	@GetMapping("/me/library")
	fun library(
		@AuthenticationPrincipal jwt: Jwt,
		@RequestParam(defaultValue = "50") limit: Int,
	): List<BookSummaryResponse> =
		bookService.library(jwt.userId(), limit).map { it.toSummaryResponse() }
}
