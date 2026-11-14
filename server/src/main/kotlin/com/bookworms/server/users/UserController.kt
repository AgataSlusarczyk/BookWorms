package com.bookworms.server.users

import com.bookworms.server.users.dto.UpdateProfileRequest
import com.bookworms.server.users.dto.UserResponse
import com.bookworms.server.common.userId
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
class UserController(
	private val userService: UserService,
) {

	@GetMapping("/me")
	fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse =
		userService.me(UUID.fromString(jwt.subject))

	// The photo as a file in a form ("multipart/form-data"), in the part called "photo". JPEG or PNG, up to 5 MB.
	@PutMapping("/me/photo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun changePhoto(@AuthenticationPrincipal jwt: Jwt, @RequestPart("photo") photo: MultipartFile): UserResponse =
		photo.inputStream.use { userService.changePhoto(jwt.userId(), it) }

	@DeleteMapping("/me/photo")
	fun removePhoto(@AuthenticationPrincipal jwt: Jwt): UserResponse =
		userService.removePhoto(jwt.userId())

	// Edit profile: name, username and email.
	@PutMapping("/me")
	fun updateProfile(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: UpdateProfileRequest): UserResponse =
		userService.updateProfile(UUID.fromString(jwt.subject), request)
}
