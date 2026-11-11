package com.bookworms.app.ui.library

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.CopyLentException
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// What happened to a change sent to the server. Each one is handled once.
internal sealed interface MyBooksEvent {
    // Edit book closes.
    data object Edited : MyBooksEvent

    // A short message (Toast).
    data class Failed(@StringRes val message: Int) : MyBooksEvent
}

// The user's books from the server. Shared by My books, My copy and Edit book (see AppNavigation),
// so a change made on one of them shows on the others right away.
internal class MyBooksViewModel(private val repository: MyBooksRepository = MyBooksRepository()) : ViewModel() {

    private val _books = MutableStateFlow<LoadState<List<OwnBook>>>(LoadState.Loading)
    val books: StateFlow<LoadState<List<OwnBook>>> = _books.asStateFlow()

    // true while a change is on its way to the server – the buttons and switches wait for it.
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = Channel<MyBooksEvent>(Channel.BUFFERED)
    val events: Flow<MyBooksEvent> = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        _books.value = LoadState.Loading
        viewModelScope.launch {
            _books.value = load { repository.myBooks() }
        }
    }

    // A book saved on Add book, so My books shows it without asking the server again. Newest go first.
    fun added(book: OwnBook) = updateBooks { listOf(book) + it }

    fun setAvailable(book: OwnBook, available: Boolean) = save {
        val changed = repository.setAvailable(book, available)
        replace(changed)
    }

    fun editBook(book: OwnBook, form: BookForm) = save {
        val changed = repository.editBook(book, form)
        replace(changed)
        _events.send(MyBooksEvent.Edited)
    }

    fun remove(book: OwnBook) = save {
        try {
            repository.remove(book.id)
        } catch (e: CopyLentException) {
            // Someone borrowed it in the meantime – the fresh list shows who, and Remove turns off.
            _events.send(MyBooksEvent.Failed(R.string.remove_lent_hint))
            refresh()
            return@save
        }
        // My copy sees the book is gone and closes.
        updateBooks { books -> books.filterNot { it.id == book.id } }
    }

    private fun replace(book: OwnBook) = updateBooks { books -> books.map { if (it.id == book.id) book else it } }

    private fun updateBooks(change: (List<OwnBook>) -> List<OwnBook>) = _books.update { state ->
        if (state is LoadState.Ready) LoadState.Ready(change(state.data)) else state
    }

    // One change at a time: a second tap while the first is saving does nothing.
    private fun save(block: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Saving a change to My books failed", e)
                _events.send(MyBooksEvent.Failed(R.string.error_save_failed))
            } finally {
                _saving.value = false
            }
        }
    }
}
