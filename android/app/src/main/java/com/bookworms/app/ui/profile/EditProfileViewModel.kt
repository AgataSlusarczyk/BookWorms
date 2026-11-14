package com.bookworms.app.ui.profile

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.EmailTakenException
import com.bookworms.app.data.InvalidPhotoException
import com.bookworms.app.data.InvalidProfileDataException
import com.bookworms.app.data.ProfileRepository
import com.bookworms.app.data.UsernameTakenException
import com.bookworms.app.data.auth.Session
import com.bookworms.app.ui.auth.isValidEmail
import com.bookworms.app.ui.auth.isValidUsername
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    // true while we wait for the server – the button shows a spinning circle.
    val saving: Boolean = false,
    val error: EditProfileError? = null,
    // true once the server saved it – the app then goes back to Profile.
    val saved: Boolean = false,
) {
    // The field errors show up only after something is typed, so an empty field isn't red at the start.
    val usernameInvalid: Boolean get() = username.isNotEmpty() && !isValidUsername(username)
    val emailInvalid: Boolean get() = email.isNotEmpty() && !isValidEmail(email)

    val canSave: Boolean
        get() = name.isNotBlank() && isValidUsername(username) && isValidEmail(email) && !saving
}

enum class EditProfileError {
    EmailTaken,
    UsernameTaken,
    // The server didn't accept a field the app let through, e.g. a very long name.
    InvalidData,
    // The picked photo couldn't be read, or the server didn't take it. The rest is saved.
    InvalidPhoto,
    ServerUnreachable,
}

class EditProfileViewModel(private val repository: ProfileRepository = ProfileRepository()) : ViewModel() {

    // The form starts with the user's data as it is now.
    private val _state = MutableStateFlow(
        Session.user.value.let { user ->
            EditProfileUiState(name = user?.name.orEmpty(), username = user?.username.orEmpty(), email = user?.email.orEmpty())
        },
    )
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    // Changing the form hides the old error from the server.
    fun onNameChange(name: String) = _state.update { it.copy(name = name, error = null) }

    // People often type "@anna" – the "@" is already shown in front, so we drop it.
    fun onUsernameChange(username: String) =
        _state.update { it.copy(username = username.removePrefix("@"), error = null) }

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, error = null) }

    // newPhoto = a photo just picked on the phone, or null to keep the old one.
    // The resolver reads the photo from the phone.
    fun save(newPhoto: Uri?, resolver: ContentResolver) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val error = try {
                repository.update(current.name, current.username, current.email)
                if (newPhoto != null) repository.changePhoto(resolver, newPhoto)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: EmailTakenException) {
                EditProfileError.EmailTaken
            } catch (e: UsernameTakenException) {
                EditProfileError.UsernameTaken
            } catch (e: InvalidProfileDataException) {
                EditProfileError.InvalidData
            } catch (e: InvalidPhotoException) {
                EditProfileError.InvalidPhoto
            } catch (e: Exception) {
                Log.w("BookWorms", "Saving the profile failed", e)
                EditProfileError.ServerUnreachable
            }
            _state.update { it.copy(saving = false, error = error, saved = error == null) }
        }
    }
}
