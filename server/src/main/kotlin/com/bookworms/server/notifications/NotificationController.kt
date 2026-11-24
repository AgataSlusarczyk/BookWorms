package com.bookworms.server.notifications

import com.bookworms.server.common.userId
import com.bookworms.server.notifications.dto.NotificationResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// The Notifications screen. Every address needs a token (see SecurityConfig).
@RestController
class NotificationController(
	private val notificationService: NotificationService,
) {

	@GetMapping("/me/notifications")
	fun myNotifications(@AuthenticationPrincipal jwt: Jwt): List<NotificationResponse> =
		notificationService.myNotifications(jwt.userId())

	// "Mark as read" at the top of the screen.
	@PostMapping("/me/notifications/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun markAllRead(@AuthenticationPrincipal jwt: Jwt) =
		notificationService.markAllRead(jwt.userId())

	// A tap on one notification. Someone else's id changes nothing, and the answer is the same,
	// so it doesn't tell whether that notification exists.
	@PostMapping("/me/notifications/{id}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun markRead(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		notificationService.markRead(jwt.userId(), id)
}
