package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

// Signing up and logging in on our server.
interface AuthApi {

    // Answers 409 (code EMAIL_TAKEN) when someone already signed up with this email.
    // Gives no token – log in after it.
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): UserDto

    // Answers 401 (code INVALID_CREDENTIALS) for a wrong email or password.
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
}

@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

// token goes into the Authorization header of the next requests (see ApiClient).
@Serializable
data class LoginResponseDto(
    val token: String,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
)
