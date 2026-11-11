package com.bookworms.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.AuthRepository
import com.bookworms.app.data.EmailTakenException
import com.bookworms.app.data.InvalidSignUpDataException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val repeatPassword: String = "",
    val termsAccepted: Boolean = false,
    // true while we wait for the server – the button shows a spinning circle.
    val creatingAccount: Boolean = false,
    val error: RegisterError? = null,
    // true once the account is created and the user is logged in – the app then opens the Library.
    val registered: Boolean = false,
) {
    // The field errors show up only after something is typed, so an empty form isn't all red.
    val emailInvalid: Boolean get() = email.isNotEmpty() && !isValidEmail(email)
    val passwordTooWeak: Boolean get() = password.isNotEmpty() && !isStrongPassword(password)
    val passwordsDontMatch: Boolean get() = repeatPassword.isNotEmpty() && repeatPassword != password

    val canCreateAccount: Boolean
        get() = name.isNotBlank() &&
            isValidEmail(email) &&
            isStrongPassword(password) &&
            repeatPassword == password &&
            termsAccepted &&
            !creatingAccount
}

enum class RegisterError {
    EmailTaken,
    // The server didn't accept a field the app let through, e.g. a very long name.
    InvalidData,
    ServerUnreachable,
}

class RegisterViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    // Changing the form hides the old error from the server.
    fun onNameChange(name: String) = _state.update { it.copy(name = name, error = null) }

    fun onEmailChange(email: String) = _state.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, error = null) }

    fun onRepeatPasswordChange(repeatPassword: String) =
        _state.update { it.copy(repeatPassword = repeatPassword, error = null) }

    fun onTermsAcceptedChange(accepted: Boolean) = _state.update { it.copy(termsAccepted = accepted, error = null) }

    fun createAccount() {
        val current = _state.value
        if (!current.canCreateAccount) return
        _state.update { it.copy(creatingAccount = true, error = null) }
        viewModelScope.launch {
            val error = try {
                repository.register(current.name, current.email, current.password)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: EmailTakenException) {
                RegisterError.EmailTaken
            } catch (e: InvalidSignUpDataException) {
                RegisterError.InvalidData
            } catch (e: Exception) {
                Log.w("BookWorms", "Signing up failed", e)
                RegisterError.ServerUnreachable
            }
            _state.update { it.copy(creatingAccount = false, error = error, registered = error == null) }
        }
    }
}
