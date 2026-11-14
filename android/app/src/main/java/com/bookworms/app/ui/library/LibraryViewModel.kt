package com.bookworms.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.load
import com.bookworms.app.ui.browse.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// "Recently added" in the Library: books the user or people in their groups have.
class LibraryViewModel(private val repository: BookRepository = BookRepository()) : ViewModel() {

    private val _books = MutableStateFlow<LoadState<List<Book>>>(LoadState.Loading)
    val books: StateFlow<LoadState<List<Book>>> = _books.asStateFlow()

    init {
        refresh()
    }

    // Books already on the screen stay there while the new list loads, so it doesn't blink.
    fun refresh() {
        if (_books.value !is LoadState.Ready) _books.value = LoadState.Loading
        viewModelScope.launch {
            _books.value = load { repository.library(limit = RecentCount) }
        }
    }

    private companion object {
        const val RecentCount = 20
    }
}
