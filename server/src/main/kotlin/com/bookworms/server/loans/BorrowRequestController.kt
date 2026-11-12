package com.bookworms.server.loans

import com.bookworms.server.common.userId
import com.bookworms.server.loans.dto.RequestResponse
import com.bookworms.server.loans.dto.SendRequestRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// Asking to borrow and answering. Every address needs a token (see SecurityConfig).
@RestController
class BorrowRequestController(
	private val requestService: BorrowRequestService,
) {

	// "Borrow" on Book details. The body is optional.
	@PostMapping("/copies/{copyId}/requests")
	@ResponseStatus(HttpStatus.CREATED)
	fun send(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable copyId: UUID,
		@Valid @RequestBody(required = false) request: SendRequestRequest?,
	): RequestResponse =
		requestService.send(jwt.userId(), copyId, request?.idempotencyKey)

	@GetMapping("/me/requests/sent")
	fun sent(@AuthenticationPrincipal jwt: Jwt): List<RequestResponse> =
		requestService.sent(jwt.userId())

	@GetMapping("/me/requests/received")
	fun received(@AuthenticationPrincipal jwt: Jwt): List<RequestResponse> =
		requestService.received(jwt.userId())

	@PostMapping("/requests/{id}/accept")
	fun accept(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): RequestResponse =
		requestService.accept(jwt.userId(), id)

	@PostMapping("/requests/{id}/reject")
	fun reject(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): RequestResponse =
		requestService.reject(jwt.userId(), id)

	@PostMapping("/requests/{id}/cancel")
	fun cancel(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): RequestResponse =
		requestService.cancel(jwt.userId(), id)
}
