package com.bookworms.server.lists

import com.bookworms.server.common.userId
import com.bookworms.server.lists.dto.ListDetailsResponse
import com.bookworms.server.lists.dto.ListRequest
import com.bookworms.server.lists.dto.ListSummaryResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// The user's own lists on My lists.
@RestController
class ListController(
	private val listService: ListService,
) {

	// bookId (optional) = for "Add to list" on Book details: each list then says whether that book is on it.
	@GetMapping("/me/lists")
	fun lists(@AuthenticationPrincipal jwt: Jwt, @RequestParam bookId: UUID?): List<ListSummaryResponse> =
		listService.lists(jwt.userId(), bookId)

	@PostMapping("/me/lists")
	@ResponseStatus(HttpStatus.CREATED)
	fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: ListRequest): ListSummaryResponse =
		listService.create(jwt.userId(), request.name, request.description)

	@GetMapping("/me/lists/{listId}")
	fun list(@AuthenticationPrincipal jwt: Jwt, @PathVariable listId: UUID): ListDetailsResponse =
		listService.list(jwt.userId(), listId)

	@PutMapping("/me/lists/{listId}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable listId: UUID,
		@Valid @RequestBody request: ListRequest,
	): ListSummaryResponse =
		listService.update(jwt.userId(), listId, request.name, request.description)

	@PutMapping("/me/lists/{listId}/books/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun addBook(@AuthenticationPrincipal jwt: Jwt, @PathVariable listId: UUID, @PathVariable bookId: UUID) =
		listService.addBook(jwt.userId(), listId, bookId)

	@DeleteMapping("/me/lists/{listId}/books/{bookId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun removeBook(@AuthenticationPrincipal jwt: Jwt, @PathVariable listId: UUID, @PathVariable bookId: UUID) =
		listService.removeBook(jwt.userId(), listId, bookId)

	@DeleteMapping("/me/lists/{listId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable listId: UUID) =
		listService.delete(jwt.userId(), listId)
}
