package com.bookworms.app.data

import com.bookworms.app.data.auth.CurrentUser
import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.AuthApi
import com.bookworms.app.data.network.LoginRequestDto
import com.bookworms.app.data.network.RegisterRequestDto
import retrofit2.HttpException

// Signing up, logging in and out. The ViewModels don't need to know about tokens.
class AuthRepository(private val api: AuthApi = ApiClient.auth) {

    // Creates the account and logs straight into it.
    // Throws EmailTakenException, InvalidSignUpDataException (the server didn't accept a field),
    // and anything else when the server can't be reached.
    suspend fun register(name: String, email: String, password: String) {
        try {
            api.register(RegisterRequestDto(name = name.trim(), email = email.trim(), password = password))
        } catch (e: HttpException) {
            throw when (e.code()) {
                409 -> EmailTakenException()
                400 -> InvalidSignUpDataException()
                else -> e
            }
        }
        login(email, password)
    }

    // Remembers the user on the phone (see Session).
    // Throws InvalidCredentialsException for a wrong email or password, and anything else when the server can't be reached.
    suspend fun login(email: String, password: String) {
        val response = try {
            api.login(LoginRequestDto(email = email.trim(), password = password))
        } catch (e: HttpException) {
            throw if (e.code() == 401) InvalidCredentialsException() else e
        }
        Session.start(
            token = response.token,
            user = CurrentUser(id = response.user.id, name = response.user.name, email = response.user.email),
        )
    }

    // The server keeps no sessions, so forgetting the token on the phone is enough.
    fun logout() = Session.end()
}

class InvalidCredentialsException : Exception("Wrong email or password")

class EmailTakenException : Exception("Email is already taken")

class InvalidSignUpDataException : Exception("The server didn't accept the sign-up data")
