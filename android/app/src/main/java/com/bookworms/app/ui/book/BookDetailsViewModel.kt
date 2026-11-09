package com.bookworms.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.load
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// One book from the server, for Book details and Add comment.
class BookDetailsViewModel(
    private val isbn: String,
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _book = MutableStateFlow<LoadState<BookDetails>>(LoadState.Loading)
    val book: StateFlow<LoadState<BookDetails>> = _book.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _book.value = LoadState.Loading
        viewModelScope.launch {
            _book.value = load { repository.details(isbn) }
        }
    }
}
