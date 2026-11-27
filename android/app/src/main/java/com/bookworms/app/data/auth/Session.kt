package com.bookworms.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bookworms.app.data.SavedData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The logged-in user.
data class CurrentUser(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    // Full address of the profile photo, or null when there's none.
    val photoUrl: String? = null,
    // A system admin: Profile shows the admin panel, and Book details the admin's buttons.
    // The server checks it again on every admin request, so this only decides what the app shows.
    val isAdmin: Boolean = false,
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
        val username = prefs.getString(KeyUsername, null)
        val email = prefs.getString(KeyUserEmail, null)
        // A login saved by an older app has no username – the user just logs in again.
        if (savedToken != null && id != null && name != null && username != null && email != null) {
            token = savedToken
            _user.value = CurrentUser(
                id,
                name,
                username,
                email,
                prefs.getString(KeyPhotoUrl, null),
                isAdmin = prefs.getBoolean(KeyIsAdmin, false),
            )
        }
    }

    fun start(token: String, user: CurrentUser) {
        prefs.edit { putString(KeyToken, token) }
        this.token = token
        update(user)
    }

    // The user changed their profile. The token stays the same.
    fun update(user: CurrentUser) {
        prefs.edit {
            putString(KeyUserId, user.id)
            putString(KeyUserName, user.name)
            putString(KeyUsername, user.username)
            putString(KeyUserEmail, user.email)
            putString(KeyPhotoUrl, user.photoUrl)
            putBoolean(KeyIsAdmin, user.isAdmin)
        }
        _user.value = user
    }

    // Logging out, or the server no longer accepts the token.
    // The books and loans saved on the phone go too, so the next person doesn't see them.
    fun end() {
        prefs.edit { clear() }
        SavedData.clear()
        token = null
        _user.value = null
    }

    private const val KeyToken = "token"
    private const val KeyUserId = "user_id"
    private const val KeyUserName = "user_name"
    private const val KeyUsername = "username"
    private const val KeyUserEmail = "user_email"
    private const val KeyPhotoUrl = "photo_url"
    private const val KeyIsAdmin = "is_admin"
}
