package com.bookworms.server.common

import com.bookworms.server.auth.ACCOUNT_BLOCKED
import com.bookworms.server.users.UserRepository
import com.bookworms.server.users.UserStatus
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// A token lasts 7 days, so a user blocked by an admin could keep using the one they have.
// This stops them on the next request: 401, which logs the app out (see ApiClient on the phone).
// Runs after the token is checked (see SecurityConfig), so it only sees requests with a valid token.
// Not a @Component on purpose – Spring Boot would also add it to every request a second time, outside security.
class BlockedUserFilter(
	private val userRepository: UserRepository,
) : OncePerRequestFilter() {

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
		val token = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
		val userId = token?.token?.subject?.let { runCatching { UUID.fromString(it) }.getOrNull() }
		if (userId != null && userRepository.findStatusById(userId) == UserStatus.BLOCKED) {
			response.status = HttpServletResponse.SC_UNAUTHORIZED
			response.contentType = MediaType.APPLICATION_JSON_VALUE
			response.characterEncoding = "UTF-8"
			response.writer.write("""{"code":"$ACCOUNT_BLOCKED","message":"This account is blocked","fieldErrors":{}}""")
			return
		}
		chain.doFilter(request, response)
	}
}
