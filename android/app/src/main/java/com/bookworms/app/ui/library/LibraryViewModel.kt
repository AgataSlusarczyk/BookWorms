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

// "Recently added" in the Library.
// TODO: the books of the picked group, once groups and copies are on the server. For now: the newest in the catalog.
class LibraryViewModel(private val repository: BookRepository = BookRepository()) : ViewModel() {

    private val _books = MutableStateFlow<LoadState<List<Book>>>(LoadState.Loading)
    val books: StateFlow<LoadState<List<Book>>> = _books.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _books.value = LoadState.Loading
        viewModelScope.launch {
            _books.value = load { repository.newest(limit = RecentCount) }
        }
    }

    private companion object {
        const val RecentCount = 20
    }
}
