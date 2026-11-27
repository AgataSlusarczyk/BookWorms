package com.bookworms.app.data

import android.content.ContentResolver
import android.net.Uri
import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.ProfileApi
import com.bookworms.app.data.network.UpdateProfileRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

// The logged-in user's own profile.
class ProfileRepository(private val api: ProfileApi = ApiClient.profile) {

    // Asks the server who the user is now and shows it everywhere in the app – e.g. the admin panel appears
    // for someone who was just made an admin, without logging out and in again.
    suspend fun refresh() {
        Session.update(api.me().toCurrentUser())
    }

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

    // Sends the photo picked on the phone to the server, and shows it everywhere in the app.
    // Throws InvalidPhotoException when the photo can't be read or the server doesn't take it,
    // and anything else when the server can't be reached.
    suspend fun changePhoto(resolver: ContentResolver, photo: Uri) {
        // Reading and shrinking a big photo takes a moment, so not on the main thread.
        val jpeg = withContext(Dispatchers.IO) { photoJpeg(resolver, photo) }
        val part = MultipartBody.Part.createFormData("photo", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
        val user = try {
            api.changePhoto(part)
        } catch (e: HttpException) {
            throw if (e.code() == 400 || e.code() == 413) InvalidPhotoException() else e
        }
        Session.update(user.toCurrentUser())
    }

    // Back to the gray circle, everywhere in the app.
    suspend fun removePhoto() {
        Session.update(api.removePhoto().toCurrentUser())
    }
}

class InvalidProfileDataException : Exception("The server didn't accept the profile data")
