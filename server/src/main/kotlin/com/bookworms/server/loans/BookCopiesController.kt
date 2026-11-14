package com.bookworms.server.loans

import com.bookworms.server.common.userId
import com.bookworms.server.loans.dto.BookCopiesResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// "Who has it" on Book details. Needs logging in, unlike the rest of /books (see SecurityConfig).
@RestController
class BookCopiesController(
	private val bookCopiesService: BookCopiesService,
) {

	@GetMapping("/books/{bookId}/copies")
	fun copies(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): BookCopiesResponse =
		bookCopiesService.copies(jwt.userId(), bookId)
}
