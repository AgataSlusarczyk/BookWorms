package com.bookworms.app.ui.library

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.InvalidIsbnException
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.MyBooksRepository
import com.bookworms.app.data.load
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal sealed interface AddBookEvent {
    // The server saved it. Add book closes.
    data class Added(val book: OwnBook) : AddBookEvent

    // A short message (Toast). The form stays, so nothing typed is lost.
    data class Failed(@StringRes val message: Int) : AddBookEvent
}

// Add book. isbn = a book from the catalog to fill the form in with ("I own this book" on Book details),
// null = an empty form.
internal class AddBookViewModel(
    private val isbn: String?,
    private val books: BookRepository = BookRepository(),
    private val myBooks: MyBooksRepository = MyBooksRepository(),
) : ViewModel() {

    // What the form starts with. null = empty.
    private val _prefill = MutableStateFlow<LoadState<BookForm?>>(LoadState.Loading)
    val prefill: StateFlow<LoadState<BookForm?>> = _prefill.asStateFlow()

    // true while the book is on its way to the server – Save waits for it.
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = Channel<AddBookEvent>(Channel.BUFFERED)
    val events: Flow<AddBookEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (isbn == null) {
            _prefill.value = LoadState.Ready(null)
            return
        }
        _prefill.value = LoadState.Loading
        viewModelScope.launch {
            val loaded = load { books.formFor(isbn) }
            // When the book isn't in the catalog (or the server is unreachable), still prefill the scanned ISBN.
            _prefill.value = if (loaded is LoadState.Ready) {
                loaded
            } else {
                LoadState.Ready(BookForm(isbn = isbn))
            }
        }
    }

    fun save(form: BookForm) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val event = try {
                AddBookEvent.Added(myBooks.add(form))
            } catch (e: CancellationException) {
                throw e
            } catch (e: InvalidIsbnException) {
                AddBookEvent.Failed(R.string.isbn_length_hint)
            } catch (e: Exception) {
                Log.w("BookWorms", "Adding a book failed", e)
                AddBookEvent.Failed(R.string.error_save_failed)
            }
            _saving.value = false
            _events.send(event)
        }
    }
}