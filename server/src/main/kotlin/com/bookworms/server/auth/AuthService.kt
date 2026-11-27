package com.bookworms.server.auth

import com.bookworms.server.auth.dto.LoginRequest
import com.bookworms.server.auth.dto.LoginResponse
import com.bookworms.server.auth.dto.RegisterRequest
import com.bookworms.server.common.ApiException
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
import com.bookworms.server.users.UserStatus
import com.bookworms.server.users.UsernameAlreadyTakenException
import com.bookworms.server.users.dto.toResponse
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
	private val tokenService: TokenService,
) {

	@Transactional
	fun register(request: RegisterRequest): User {
		val email = request.email.lowercase()
		if (userRepository.existsByEmail(email)) {
			throw EmailAlreadyTakenException()
		}
		val username = request.username.lowercase()
		if (userRepository.existsByUsername(username)) {
			throw UsernameAlreadyTakenException()
		}
		val user = User(
			name = request.name.trim(),
			username = username,
			email = email,
			passwordHash = passwordEncoder.encode(request.password)!!,
		)
		return userRepository.save(user)
	}

	@Transactional(readOnly = true)
	fun login(request: LoginRequest): LoginResponse {
		val user = userRepository.findByEmail(request.email.lowercase())
		if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
			throw InvalidCredentialsException()
		}
		// Only after the password, so nobody learns that an email is blocked without knowing the password.
		if (user.status == UserStatus.BLOCKED) {
			throw AccountBlockedException()
		}
		return LoginResponse(
			token = tokenService.createToken(user),
			user = user.toResponse(),
		)
	}
}

class EmailAlreadyTakenException :
	ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email is already taken")

class InvalidCredentialsException :
	ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password")

// A system admin blocked the account. 403 at login; 401 with the same code for a token given out before (BlockedUserFilter).
class AccountBlockedException :
	ApiException(HttpStatus.FORBIDDEN, ACCOUNT_BLOCKED, "This account is blocked")

const val ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED"
