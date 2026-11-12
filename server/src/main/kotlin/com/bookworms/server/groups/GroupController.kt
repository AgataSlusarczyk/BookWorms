package com.bookworms.server.groups

import com.bookworms.server.common.userId
import com.bookworms.server.groups.dto.GroupBookResponse
import com.bookworms.server.groups.dto.GroupDetailsResponse
import com.bookworms.server.groups.dto.GroupRequest
import com.bookworms.server.groups.dto.GroupSummaryResponse
import com.bookworms.server.groups.dto.InvitePreviewResponse
import com.bookworms.server.groups.dto.JoinGroupRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// Groups. Every address needs a token (see SecurityConfig).
@RestController
class GroupController(
	private val groupService: GroupService,
) {

	@GetMapping("/me/groups")
	fun myGroups(@AuthenticationPrincipal jwt: Jwt): List<GroupSummaryResponse> =
		groupService.myGroups(jwt.userId())

	@PostMapping("/groups")
	@ResponseStatus(HttpStatus.CREATED)
	fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: GroupRequest): GroupDetailsResponse =
		groupService.create(jwt.userId(), request)

	// "Find group" – the code goes in the body, not the address, so it doesn't end up in server logs.
	@PostMapping("/groups/invite")
	fun preview(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: JoinGroupRequest): InvitePreviewResponse =
		groupService.preview(jwt.userId(), request.code)

	@PostMapping("/groups/join")
	fun join(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: JoinGroupRequest): GroupDetailsResponse =
		groupService.join(jwt.userId(), request.code)

	@GetMapping("/groups/{id}")
	fun details(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): GroupDetailsResponse =
		groupService.details(jwt.userId(), id)

	@GetMapping("/groups/{id}/books")
	fun books(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): List<GroupBookResponse> =
		groupService.books(jwt.userId(), id)

	// Admin only.
	@PatchMapping("/groups/{id}")
	fun update(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@Valid @RequestBody request: GroupRequest,
	): GroupDetailsResponse =
		groupService.update(jwt.userId(), id, request)

	// Admin only.
	@DeleteMapping("/groups/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		groupService.delete(jwt.userId(), id)

	// The admin, or anyone when the admin let members invite.
	@PostMapping("/groups/{id}/invite-code")
	fun newInviteCode(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): GroupDetailsResponse =
		groupService.newInviteCode(jwt.userId(), id)

	// Admin only.
	@PostMapping("/groups/{id}/members/{memberId}/admin")
	fun makeAdmin(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@PathVariable memberId: UUID,
	): GroupDetailsResponse =
		groupService.makeAdmin(jwt.userId(), id, memberId)

	// Admin only, and not for other admins.
	@DeleteMapping("/groups/{id}/members/{memberId}")
	fun removeMember(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@PathVariable memberId: UUID,
	): GroupDetailsResponse =
		groupService.removeMember(jwt.userId(), id, memberId)

	@DeleteMapping("/me/groups/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun leave(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		groupService.leave(jwt.userId(), id)
}
