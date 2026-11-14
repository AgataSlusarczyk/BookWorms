package com.bookworms.server.users

import com.bookworms.server.auth.EmailAlreadyTakenException
import com.bookworms.server.common.ApiException
import com.bookworms.server.users.dto.UpdateProfileRequest
import com.bookworms.server.users.dto.UserResponse
import com.bookworms.server.users.dto.toResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.UUID

// 3–20 letters, digits, dots or underscores – the same rule as in the app.
const val USERNAME_PATTERN = "[A-Za-z0-9._]{3,20}"

@Service
class UserService(
	private val userRepository: UserRepository,
	private val photoFiles: PhotoFiles,
) {

	@Transactional(readOnly = true)
	fun me(userId: UUID): UserResponse = user(userId).toResponse()

	// The email and username stay unique: someone else's gives 409. Keeping your own is fine.
	// The token holds only the user's id, so a new email doesn't log anyone out.
	@Transactional
	fun updateProfile(userId: UUID, request: UpdateProfileRequest): UserResponse {
		val user = user(userId)
		val email = request.email.trim().lowercase()
		val username = request.username.lowercase()
		if (email != user.email && userRepository.existsByEmail(email)) {
			throw EmailAlreadyTakenException()
		}
		if (username != user.username && userRepository.existsByUsername(username)) {
			throw UsernameAlreadyTakenException()
		}
		user.name = request.name.trim()
		user.username = username
		user.email = email
		return user.toResponse()
	}

	// A new profile photo replaces the old one, whose file is removed.
	@Transactional
	fun changePhoto(userId: UUID, photo: InputStream): UserResponse {
		val user = user(userId)
		val old = user.photoUrl
		user.photoUrl = photoFiles.save(photo)
		photoFiles.delete(old)
		return user.toResponse()
	}

	// No photo is fine too.
	@Transactional
	fun removePhoto(userId: UUID): UserResponse {
		val user = user(userId)
		photoFiles.delete(user.photoUrl)
		user.photoUrl = null
		return user.toResponse()
	}

	private fun user(userId: UUID): User =
		userRepository.findByIdOrNull(userId)
			?: throw ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User no longer exists")
}

class UsernameAlreadyTakenException :
	ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "Username is already taken")
