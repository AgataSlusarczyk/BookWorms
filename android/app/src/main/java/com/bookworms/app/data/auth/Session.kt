package com.bookworms.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The logged-in user.
data class CurrentUser(
    val id: String,
    val name: String,
    val email: String,
)

// Who is logged in, and the token that proves it to the server.
// Kept on the phone, so the user stays logged in after closing the app – until they log out
// or the token runs out (the server gives it for 7 days).
object Session {

    private lateinit var prefs: SharedPreferences

    private val _user = MutableStateFlow<CurrentUser?>(null)

    // null = nobody is logged in.
    val user: StateFlow<CurrentUser?> = _user.asStateFlow()

    // Read by ApiClient on every request, from OkHttp's threads.
    @Volatile
    var token: String? = null
        private set

    // Called once, when the app starts (BookWormsApp).
    fun init(context: Context) {
        prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
        val savedToken = prefs.getString(KeyToken, null)
        val id = prefs.getString(KeyUserId, null)
        val name = prefs.getString(KeyUserName, null)
        val email = prefs.getString(KeyUserEmail, null)
        if (savedToken != null && id != null && name != null && email != null) {
            token = savedToken
            _user.value = CurrentUser(id, name, email)
        }
    }

    fun start(token: String, user: CurrentUser) {
        prefs.edit {
            putString(KeyToken, token)
            putString(KeyUserId, user.id)
            putString(KeyUserName, user.name)
            putString(KeyUserEmail, user.email)
        }
        this.token = token
        _user.value = user
    }

    // Logging out, or the server no longer accepts the token.
    fun end() {
        prefs.edit { clear() }
        token = null
        _user.value = null
    }

    private const val KeyToken = "token"
    private const val KeyUserId = "user_id"
    private const val KeyUserName = "user_name"
    private const val KeyUserEmail = "user_email"
}
