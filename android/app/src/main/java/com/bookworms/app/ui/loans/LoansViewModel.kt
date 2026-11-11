package com.bookworms.app.ui.loans

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.CannotAnswerException
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.LoansRepository
import com.bookworms.app.data.load
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

// The Loans tabs: borrowed books, sent requests, lent books and requests to answer – all from the server.
internal class LoansViewModel(private val repository: LoansRepository = LoansRepository()) : ViewModel() {

    private val _loans = MutableStateFlow<LoadState<Loans>>(LoadState.Loading)
    val loans: StateFlow<LoadState<Loans>> = _loans.asStateFlow()

    // true while an answer or "Mark as returned" is on its way – the buttons wait for it.
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    // Short messages (Toast, string ids) about what didn't work, each shown once.
    private val _errors = Channel<Int>(Channel.BUFFERED)
    val errors: Flow<Int> = _errors.receiveAsFlow()

    // Called each time the tab opens, so it shows what changed in the meantime (e.g. a new request).
    // The lists already on screen stay there while the fresh ones load.
    fun refresh() {
        if (_loans.value !is LoadState.Ready) _loans.value = LoadState.Loading
        viewModelScope.launch {
            val fresh = load { repository.loans() }
            // A failed quiet refresh keeps the old lists instead of hiding them behind "Try again".
            if (fresh is LoadState.Ready || _loans.value !is LoadState.Ready) _loans.value = fresh
        }
    }

    fun markReturned(loan: Loan) = save { repository.markReturned(loan.id) }

    fun accept(request: BookRequest) = save { repository.accept(request.id) }

    fun reject(request: BookRequest) = save { repository.reject(request.id) }

    // After a change all lists are asked for again: accepting moves a book to Lent and may change
    // whether the other requests for it can be accepted.
    private fun save(block: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CannotAnswerException) {
                // Answered on another phone, cancelled by the person who asked, or the book is lent out already.
                _errors.send(R.string.error_request_changed)
            } catch (e: Exception) {
                Log.w("BookWorms", "Saving a change in Loans failed", e)
                _errors.send(R.string.error_save_failed)
            }
            val fresh = load { repository.loans() }
            if (fresh is LoadState.Ready) _loans.value = fresh
            _saving.value = false
        }
    }
}
