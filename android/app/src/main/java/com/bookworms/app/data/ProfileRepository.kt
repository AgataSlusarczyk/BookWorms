package com.bookworms.app.data

import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.ProfileApi
import com.bookworms.app.data.network.UpdateProfileRequestDto
import retrofit2.HttpException

// The logged-in user's own profile.
class ProfileRepository(private val api: ProfileApi = ApiClient.profile) {

    // Saves the new name, username and email, and shows them everywhere in the app (see Session).
    // Throws EmailTakenException, UsernameTakenException, InvalidProfileDataException (the server didn't accept a field),
    // and anything else when the server can't be reached.
    suspend fun update(name: String, username: String, email: String) {
        val user = try {
            api.updateProfile(UpdateProfileRequestDto(name = name.trim(), username = username.trim(), email = email.trim()))
        } catch (e: HttpException) {
            throw when (e.code()) {
                409 -> takenException(e)
                400 -> InvalidProfileDataException()
                else -> e
            }
        }
        Session.update(user.toCurrentUser())
    }
}

class InvalidProfileDataException : Exception("The server didn't accept the profile data")
