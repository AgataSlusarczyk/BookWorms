package com.bookworms.server.loans

import com.bookworms.server.common.userId
import com.bookworms.server.loans.dto.LoanResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

// The Borrowing and Lent tabs, and "Mark as returned". Every address needs a token (see SecurityConfig).
@RestController
class BorrowingController(
	private val borrowingService: BorrowingService,
) {

	@GetMapping("/me/borrowings")
	fun myBorrowings(@AuthenticationPrincipal jwt: Jwt): List<LoanResponse> =
		borrowingService.myBorrowings(jwt.userId())

	@GetMapping("/me/lent")
	fun myLent(@AuthenticationPrincipal jwt: Jwt): List<LoanResponse> =
		borrowingService.myLent(jwt.userId())

	@PostMapping("/borrowings/{id}/return")
	fun markReturned(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): LoanResponse =
		borrowingService.markReturned(jwt.userId(), id)
}
