package com.bookworms.server.shelves

import com.bookworms.server.common.userId
import com.bookworms.server.shelves.dto.MyShelfStateResponse
import com.bookworms.server.shelves.dto.SetReadingStatusRequest
import com.bookworms.server.shelves.dto.ShelfBookResponse
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

// Favorites and the reading status. Each change answers with the book's heart and status after it.
@RestController
class ShelfController(
	private val shelfService: ShelfService,
) {

	@GetMapping("/me/shelf")
	fun shelf(@AuthenticationPrincipal jwt: Jwt): List<ShelfBookResponse> =
		shelfService.shelf(jwt.userId())

	@GetMapping("/me/shelf/{bookId}")
	fun book(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): MyShelfStateResponse =
		shelfService.book(jwt.userId(), bookId)

	@PutMapping("/me/shelf/{bookId}/status")
	fun setStatus(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable bookId: UUID,
		@Valid @RequestBody request: SetReadingStatusRequest,
	): MyShelfStateResponse =
		shelfService.setStatus(jwt.userId(), bookId, request.status!!)

	@DeleteMapping("/me/shelf/{bookId}/status")
	fun removeStatus(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): MyShelfStateResponse =
		shelfService.removeStatus(jwt.userId(), bookId)

	@PutMapping("/me/shelf/{bookId}/favorite")
	fun addFavorite(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): MyShelfStateResponse =
		shelfService.addFavorite(jwt.userId(), bookId)

	@DeleteMapping("/me/shelf/{bookId}/favorite")
	fun removeFavorite(@AuthenticationPrincipal jwt: Jwt, @PathVariable bookId: UUID): MyShelfStateResponse =
		shelfService.removeFavorite(jwt.userId(), bookId)
}
