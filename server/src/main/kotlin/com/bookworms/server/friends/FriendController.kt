package com.bookworms.server.friends

import com.bookworms.server.common.userId
import com.bookworms.server.friends.dto.FriendBookResponse
import com.bookworms.server.friends.dto.FriendRequestResponse
import com.bookworms.server.friends.dto.FriendResponse
import com.bookworms.server.friends.dto.SendFriendRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// Friends. Every address needs a token (see SecurityConfig).
@RestController
class FriendController(
	private val friendService: FriendService,
) {

	@GetMapping("/friends")
	fun listFriends(@AuthenticationPrincipal jwt: Jwt): List<FriendResponse> =
		friendService.listFriends(jwt.userId())

	@GetMapping("/friends/{friendId}/books")
	fun listFriendBooks(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable friendId: UUID,
	): List<FriendBookResponse> =
		friendService.listFriendBooks(jwt.userId(), friendId)

	@GetMapping("/friends/requests")
	fun listIncomingRequests(@AuthenticationPrincipal jwt: Jwt): List<FriendRequestResponse> =
		friendService.listIncomingRequests(jwt.userId())

	@GetMapping("/friends/search")
	fun searchUsers(
		@AuthenticationPrincipal jwt: Jwt,
		@RequestParam query: String,
	): List<FriendResponse> =
		friendService.searchUsers(jwt.userId(), query)

	@PostMapping("/friends/requests")
	@ResponseStatus(HttpStatus.CREATED)
	fun sendRequest(
		@AuthenticationPrincipal jwt: Jwt,
		@RequestBody request: SendFriendRequest,
	) = friendService.sendRequest(jwt.userId(), request.userId)

	@PostMapping("/friends/requests/{id}/accept")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun acceptRequest(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		friendService.acceptRequest(jwt.userId(), id)

	@PostMapping("/friends/requests/{id}/reject")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun rejectRequest(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		friendService.rejectRequest(jwt.userId(), id)

	@DeleteMapping("/friends/{friendId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun removeFriend(@AuthenticationPrincipal jwt: Jwt, @PathVariable friendId: UUID) =
		friendService.removeFriend(jwt.userId(), friendId)
}
