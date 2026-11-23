package com.bookworms.server.groups

import com.bookworms.server.common.userId
import com.bookworms.server.friends.dto.FriendResponse
import com.bookworms.server.groups.dto.GroupBookResponse
import com.bookworms.server.groups.dto.GroupDetailsResponse
import com.bookworms.server.groups.dto.GroupInvitationResponse
import com.bookworms.server.groups.dto.GroupRequest
import com.bookworms.server.groups.dto.GroupSummaryResponse
import com.bookworms.server.groups.dto.InvitePreviewResponse
import com.bookworms.server.groups.dto.InviteUserRequest
import com.bookworms.server.groups.dto.JoinGroupRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

// Groups. Every address needs a token (see SecurityConfig).
@RestController
class GroupController(
	private val groupService: GroupService,
) {

	// People to invite: by username, without members and people invited already.
	@GetMapping("/groups/{id}/search-users")
	fun searchInvitableUsers(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestParam query: String,
	): List<FriendResponse> =
		groupService.searchInvitableUsers(jwt.userId(), id, query)

	@PostMapping("/groups/{id}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	fun inviteUser(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestBody request: InviteUserRequest,
	) = groupService.inviteUser(jwt.userId(), id, request.userId)

	@GetMapping("/me/group-invitations")
	fun myInvitations(@AuthenticationPrincipal jwt: Jwt): List<GroupInvitationResponse> =
		groupService.myInvitations(jwt.userId())

	@PostMapping("/me/group-invitations/{invitationId}/accept")
	fun acceptInvitation(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable invitationId: UUID,
	): GroupDetailsResponse =
		groupService.acceptInvitation(jwt.userId(), invitationId)

	@PostMapping("/me/group-invitations/{invitationId}/reject")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun rejectInvitation(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable invitationId: UUID,
	) = groupService.rejectInvitation(jwt.userId(), invitationId)

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

	// Admin only. The photo as a file in a form, in the part called "photo" – like the profile photo.
	@PutMapping("/groups/{id}/photo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun changePhoto(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@RequestPart("photo") photo: MultipartFile,
	): GroupDetailsResponse =
		photo.inputStream.use { groupService.changePhoto(jwt.userId(), id, it) }

	// Admin only.
	@DeleteMapping("/groups/{id}/photo")
	fun removePhoto(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): GroupDetailsResponse =
		groupService.removePhoto(jwt.userId(), id)

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
