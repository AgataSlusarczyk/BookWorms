package com.bookworms.server.auth

import com.bookworms.server.auth.dto.RegisterRequest
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.ResponseStatus

@Service
class AuthService(
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
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
}

@ResponseStatus(HttpStatus.CONFLICT, reason = "Email is already taken")
class EmailAlreadyTakenException : RuntimeException()
