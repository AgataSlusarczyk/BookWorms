package com.bookworms.server.auth

import com.bookworms.server.auth.dto.LoginRequest
import com.bookworms.server.auth.dto.LoginResponse
import com.bookworms.server.auth.dto.RegisterRequest
import com.bookworms.server.common.ApiException
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
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
		val user = User(
			name = request.name.trim(),
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
