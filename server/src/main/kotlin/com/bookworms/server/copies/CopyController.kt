package com.bookworms.server.copies

import com.bookworms.server.common.userId
import com.bookworms.server.copies.dto.AddCopyRequest
import com.bookworms.server.copies.dto.ChangeAvailabilityRequest
import com.bookworms.server.copies.dto.ChangeGroupsRequest
import com.bookworms.server.copies.dto.CopyResponse
import com.bookworms.server.copies.dto.EditBookRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// The logged-in user's own books. Every address needs a token (see SecurityConfig).
@RestController
class CopyController(
	private val copyService: CopyService,
) {

	@GetMapping("/me/copies")
	fun myCopies(@AuthenticationPrincipal jwt: Jwt): List<CopyResponse> =
		copyService.myCopies(jwt.userId())

	@PostMapping("/me/copies")
	@ResponseStatus(HttpStatus.CREATED)
	fun addCopy(
		@AuthenticationPrincipal jwt: Jwt,
		@Valid @RequestBody request: AddCopyRequest,
	): CopyResponse =
		copyService.addCopy(jwt.userId(), request)

	@PatchMapping("/copies/{id}")
	fun changeAvailability(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestBody request: ChangeAvailabilityRequest,
	): CopyResponse =
		copyService.changeAvailability(jwt.userId(), id, request.availability)

	// Only the user's own groups – any other answers 400 (code NOT_GROUP_MEMBER).
	@PutMapping("/copies/{id}/groups")
	fun changeGroups(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestBody request: ChangeGroupsRequest,
	): CopyResponse =
		copyService.changeGroups(jwt.userId(), id, request.groupIds)

	@PutMapping("/copies/{id}/book")
	fun editBook(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@Valid @RequestBody request: EditBookRequest,
	): CopyResponse =
		copyService.editBook(jwt.userId(), id, request)

	@DeleteMapping("/copies/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun removeCopy(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
	) {
		copyService.removeCopy(jwt.userId(), id)
	}
}
