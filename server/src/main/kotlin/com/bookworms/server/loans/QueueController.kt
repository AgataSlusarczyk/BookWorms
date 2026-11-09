package com.bookworms.server.loans

import com.bookworms.server.common.userId
import com.bookworms.server.loans.dto.QueueResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// "Join queue" and "Leave queue" on Book details. Every address needs a token (see SecurityConfig).
@RestController
class QueueController(
	private val queueService: QueueService,
) {

	@PostMapping("/copies/{copyId}/queue")
	fun join(@AuthenticationPrincipal jwt: Jwt, @PathVariable copyId: UUID): QueueResponse =
		queueService.join(jwt.userId(), copyId)

	@DeleteMapping("/copies/{copyId}/queue")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun leave(@AuthenticationPrincipal jwt: Jwt, @PathVariable copyId: UUID) =
		queueService.leave(jwt.userId(), copyId)

	@GetMapping("/me/queue")
	fun myQueues(@AuthenticationPrincipal jwt: Jwt): List<QueueResponse> =
		queueService.myQueues(jwt.userId())
}
