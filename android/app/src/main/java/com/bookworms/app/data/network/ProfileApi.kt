package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PUT

// The logged-in user's own profile.
interface ProfileApi {

    // Sends all three fields, changed or not. Answers 409 with code EMAIL_TAKEN or USERNAME_TAKEN
    // when someone else already has them, and 400 when a field isn't accepted.
    @PUT("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserDto
}

@Serializable
data class UpdateProfileRequestDto(
    val name: String,
    val username: String,
    val email: String,
)
