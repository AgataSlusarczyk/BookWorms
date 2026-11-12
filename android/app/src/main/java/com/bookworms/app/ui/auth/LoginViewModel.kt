package com.bookworms.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.AuthRepository
import com.bookworms.app.data.InvalidCredentialsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    // true while we wait for the server – the button shows a spinning circle.
    val loggingIn: Boolean = false,
    val error: LoginError? = null,
    // true once the server accepted the email and password – the app then opens the Library.
    val loggedIn: Boolean = false,
) {
    val canLogIn: Boolean get() = email.isNotBlank() && password.isNotEmpty() && !loggingIn
}

enum class LoginError {
    InvalidCredentials,
    ServerUnreachable,
}

class LoginViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    // Typing again hides the old error.
    fun onEmailChange(email: String) = _state.update { it.copy(email = email, error = null) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password, error = null) }

    fun login() {
        val current = _state.value
        if (!current.canLogIn) return
        _state.update { it.copy(loggingIn = true, error = null) }
        viewModelScope.launch {
            val error = try {
                repository.login(current.email, current.password)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidCredentialsException) {
                LoginError.InvalidCredentials
            } catch (e: Exception) {
                Log.w("BookWorms", "Logging in failed", e)
                LoginError.ServerUnreachable
            }
            _state.update { it.copy(loggingIn = false, error = error, loggedIn = error == null) }
        }
    }
}
