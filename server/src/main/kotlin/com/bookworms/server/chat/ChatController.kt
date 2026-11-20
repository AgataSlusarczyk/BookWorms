package com.bookworms.server.chat

import com.bookworms.server.chat.dto.GroupMessageResponse
import com.bookworms.server.chat.dto.SendMessageRequest
import com.bookworms.server.common.userId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

// The group's chat. Every address needs a token (see SecurityConfig).
@RestController
class ChatController(
	private val chatService: ChatService,
) {

	// The newest first. For older ones: before = the id of the oldest message the app has.
	@GetMapping("/groups/{id}/messages")
	fun messages(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestParam(required = false) before: UUID?,
		@RequestParam(defaultValue = "50") limit: Int,
	): List<GroupMessageResponse> =
		chatService.messages(jwt.userId(), id, before, limit)

	@PostMapping("/groups/{id}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	fun send(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@Valid @RequestBody request: SendMessageRequest,
	): GroupMessageResponse =
		chatService.send(jwt.userId(), id, request)

	// Stays open (Server-Sent Events): every new message comes as an event named "message",
	// with the same JSON as above. The app opens it again when it ends.
	@GetMapping("/groups/{id}/messages/stream")
	fun stream(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): SseEmitter =
		chatService.stream(jwt.userId(), id)
}
