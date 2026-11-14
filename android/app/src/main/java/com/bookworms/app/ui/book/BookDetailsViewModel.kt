package com.bookworms.app.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.load
import com.bookworms.app.data.withReviews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One book from the server, for Book details and Add comment.
class BookDetailsViewModel(
    private val isbn: String,
    private val repository: BookRepository = BookRepository(),
) : ViewModel() {

    private val _book = MutableStateFlow<LoadState<BookDetails>>(LoadState.Loading)
    val book: StateFlow<LoadState<BookDetails>> = _book.asStateFlow()

    // true when the last rating didn't reach the server. The screen says so once, then calls ratingErrorShown().
    private val _ratingFailed = MutableStateFlow(false)
    val ratingFailed: StateFlow<Boolean> = _ratingFailed.asStateFlow()

    private var rating: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        _book.value = LoadState.Loading
        viewModelScope.launch {
            _book.value = load { repository.details(isbn) }
        }
    }

    // stars = 1–5, or null to take the rating back.
    // The stars change on the screen right away; if the server doesn't take them, they go back.
    fun rate(stars: Int?) {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        val before = book.ratings
        updateBook { it.copy(ratings = it.ratings.copy(mine = stars)) }
        // A quick second tap wins – the first one is dropped, so the answers can't come back in the wrong order.
        rating?.cancel()
        rating = viewModelScope.launch {
            try {
                val reviews = repository.rate(book.id, stars)
                updateBook { it.withReviews(reviews) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Rating the book failed", e)
                updateBook { it.copy(ratings = before) }
                _ratingFailed.value = true
            }
        }
    }

    fun ratingErrorShown() {
        _ratingFailed.value = false
    }

    private fun updateBook(change: (BookDetails) -> BookDetails) =
        _book.update { state -> if (state is LoadState.Ready) LoadState.Ready(change(state.data)) else state }
}
