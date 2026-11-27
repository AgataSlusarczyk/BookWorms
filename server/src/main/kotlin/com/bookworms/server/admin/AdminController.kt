package com.bookworms.server.admin

import com.bookworms.server.admin.dto.AdminGroupResponse
import com.bookworms.server.admin.dto.AdminLogResponse
import com.bookworms.server.admin.dto.AdminStatsResponse
import com.bookworms.server.admin.dto.AdminUserResponse
import com.bookworms.server.books.dto.BookDetailsResponse
import com.bookworms.server.common.userId
import com.bookworms.server.copies.dto.EditBookRequest
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// The admin panel and the admin's extra buttons on the other screens. A token is needed (see SecurityConfig),
// and AdminService answers 403 (code NOT_SYSTEM_ADMIN) to anyone who isn't a system admin.
@RestController
@RequestMapping("/admin")
class AdminController(
	private val adminService: AdminService,
) {

	@GetMapping("/stats")
	fun stats(@AuthenticationPrincipal jwt: Jwt): AdminStatsResponse =
		adminService.stats(jwt.userId())

	@GetMapping("/users")
	fun users(@AuthenticationPrincipal jwt: Jwt, @RequestParam(required = false) query: String?): List<AdminUserResponse> =
		adminService.users(jwt.userId(), query)

	@PostMapping("/users/{id}/block")
	fun block(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): AdminUserResponse =
		adminService.block(jwt.userId(), id)

	@PostMapping("/users/{id}/unblock")
	fun unblock(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): AdminUserResponse =
		adminService.unblock(jwt.userId(), id)

	@GetMapping("/groups")
	fun groups(@AuthenticationPrincipal jwt: Jwt): List<AdminGroupResponse> =
		adminService.groups(jwt.userId())

	@DeleteMapping("/groups/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteGroup(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		adminService.deleteGroup(jwt.userId(), id)

	// Book details' "Edit book" for an admin. The same form as editing your own typed-in book.
	@PutMapping("/books/{id}")
	fun editBook(
		@AuthenticationPrincipal jwt: Jwt,
		@PathVariable id: UUID,
		@Valid @RequestBody request: EditBookRequest,
	): BookDetailsResponse =
		adminService.editBook(jwt.userId(), id, request)

	@DeleteMapping("/books/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun deleteBook(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		adminService.deleteBook(jwt.userId(), id)

	@PostMapping("/reviews/{id}/hide")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	fun hideReview(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) =
		adminService.hideReview(jwt.userId(), id)

	@GetMapping("/logs")
	fun logs(@AuthenticationPrincipal jwt: Jwt): List<AdminLogResponse> =
		adminService.logs(jwt.userId())
}
