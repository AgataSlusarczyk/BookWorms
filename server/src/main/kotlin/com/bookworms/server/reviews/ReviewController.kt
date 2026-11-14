package com.bookworms.server.reviews

import com.bookworms.server.common.userId
import com.bookworms.server.reviews.dto.BookReviewsResponse
import com.bookworms.server.reviews.dto.RateBookRequest
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// Ratings on Book details. Reading them works without logging in (GET /books/** is public, see SecurityConfig);
// rating needs a token.
@RestController
class ReviewController(
	private val reviewService: ReviewService,
) {

	// jwt is null for someone without an account – they just don't get myRating.
	@GetMapping("/books/{bookId}/reviews")
	fun reviews(@AuthenticationPrincipal jwt: Jwt?, @PathVariable bookId: UUID): BookReviewsResponse =
		reviewService.reviews(bookId, jwt?.userId())

	// Answers with the ratings after the change, so the app can show the new average right away.
	@PutMapping("/books/{bookId}/review")
	fun rate(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable bookId: UUID,
		@Valid @RequestBody request: RateBookRequest,
	): BookReviewsResponse =
		reviewService.rate(bookId, jwt.userId(), request)

	@DeleteMapping("/books/{bookId}/review")
	fun removeRating(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): BookReviewsResponse =
		reviewService.removeRating(bookId, jwt.userId())
}
