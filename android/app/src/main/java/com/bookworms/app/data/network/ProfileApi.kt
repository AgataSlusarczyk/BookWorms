package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part

// The logged-in user's own profile.
interface ProfileApi {

    // Sends all three fields, changed or not. Answers 409 with code EMAIL_TAKEN or USERNAME_TAKEN
    // when someone else already has them, and 400 when a field isn't accepted.
    @PUT("me")
    suspend fun updateProfile(@Body request: UpdateProfileRequestDto): UserDto

    // The photo as a file in a form, in the part called "photo". Answers 400 when it isn't a picture
    // and 413 when it's too big. The old photo is removed.
    @Multipart
    @PUT("me/photo")
    suspend fun changePhoto(@Part photo: MultipartBody.Part): UserDto

    @DELETE("me/photo")
    suspend fun removePhoto(): UserDto
}

@Serializable
data class UpdateProfileRequestDto(
    val name: String,
    val username: String,
    val email: String,
)
