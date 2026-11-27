package com.bookworms.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.MyBooksRepository
import com.bookworms.app.data.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: MyBooksRepository = MyBooksRepository(),
    private val profile: ProfileRepository = ProfileRepository(),
) : ViewModel() {

    // null until the server answers, so no made-up number shows up for a moment.
    private val _booksCount = MutableStateFlow<Int?>(null)
    val booksCount: StateFlow<Int?> = _booksCount.asStateFlow()

    // Asked every time Profile opens, because the user may have just added or removed a book in My books.
    fun refresh() {
        viewModelScope.launch {
            try {
                _booksCount.value = repository.count()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Not worth an error on the screen – the number just stays as it was.
                Log.w("BookWorms", "Counting my books failed", e)
            }
        }
        // The name, photo and admin role as the server has them – they may have changed on another phone or by an admin.
        viewModelScope.launch {
            try {
                profile.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // What's saved on the phone stays.
                Log.w("BookWorms", "Refreshing the profile failed", e)
            }
        }
    }
}
