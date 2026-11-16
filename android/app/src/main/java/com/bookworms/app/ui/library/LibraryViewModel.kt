package com.bookworms.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.MyBooksRepository
import com.bookworms.app.data.load
import com.bookworms.app.ui.browse.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// "Recently added" in the Library: books the user or people in their groups have.
class LibraryViewModel(
    private val repository: BookRepository = BookRepository(),
    private val myBooks: MyBooksRepository = MyBooksRepository(),
) : ViewModel() {

    private val _books = MutableStateFlow<LoadState<List<Book>>>(LoadState.Loading)
    val books: StateFlow<LoadState<List<Book>>> = _books.asStateFlow()

    // When the server can't be reached: the user's own books saved on the phone, shown instead.
    // null = the server answered, or there's no copy on the phone.
    private val _savedBooks = MutableStateFlow<List<OwnBook>?>(null)
    internal val savedBooks: StateFlow<List<OwnBook>?> = _savedBooks.asStateFlow()

    // true while the server is being asked again and the saved books are on the screen.
    private val _checking = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    init {
        refresh()
    }

    // Books already on the screen stay there while the new list loads, so it doesn't blink.
    // The saved books stay on the screen while the server is asked again, instead of a spinning circle.
    fun refresh() {
        if (_books.value !is LoadState.Ready && _savedBooks.value == null) _books.value = LoadState.Loading
        _checking.value = true
        viewModelScope.launch {
            val fresh = load { repository.library(limit = RecentCount) }
            _savedBooks.value = if (fresh is LoadState.Failed) myBooks.savedMyBooks() else null
            _books.value = fresh
            _checking.value = false
            // With the server there: the user's books are saved on the phone again, so they're
            // there next time it can't be reached – even if the user never opened My books.
            if (fresh is LoadState.Ready) load { myBooks.myBooks() }
        }
    }

    private companion object {
        const val RecentCount = 20
    }
}
