package com.bookworms.app.ui.notifications

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.CannotAnswerException
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.LoansRepository
import com.bookworms.app.data.NotificationsRepository
import com.bookworms.app.data.load
import com.bookworms.app.ui.loans.BookRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Everything the Notifications screen shows.
internal data class NotificationsData(
    // Newest first.
    val items: List<NotificationItem>,
    // Requests to the user still waiting for an answer, by id. A "wants to borrow" card whose request is here
    // gets Accept and Reject; one that isn't (answered on Loans, or taken back) has no buttons.
    val openRequests: Map<String, BookRequest>,
)

// The Notifications screen: the user's notifications from the server, and answers to borrow requests right there.
internal class NotificationsViewModel(
    private val notifications: NotificationsRepository = NotificationsRepository(),
    private val loans: LoansRepository = LoansRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<NotificationsData>>(LoadState.Loading)
    val state: StateFlow<LoadState<NotificationsData>> = _state.asStateFlow()

    // Answers given on this screen, by request id: true = accepted. The card then says "You accepted".
    private val _answers = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val answers: StateFlow<Map<String, Boolean>> = _answers.asStateFlow()

    // true while an answer is on its way – the buttons wait for it.
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    // Short messages (Toast, string ids) about what didn't work, each shown once.
    private val _errors = Channel<Int>(Channel.BUFFERED)
    val errors: Flow<Int> = _errors.receiveAsFlow()

    // true while pull to refresh is asking the server – the little spinning circle at the top.
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    // After all the fields above: Kotlin sets them top to bottom, and refresh() needs them all.
    init {
        refresh()
    }

    // The list on screen stays while a fresh one loads (pull to refresh).
    fun refresh() {
        if (_refreshing.value) return
        if (_state.value !is LoadState.Ready) _state.value = LoadState.Loading
        _refreshing.value = true
        viewModelScope.launch {
            val fresh = load { fetch() }
            if (fresh is LoadState.Ready) {
                _state.value = fresh
            } else if (_state.value is LoadState.Ready) {
                // The old list stays; just say the server couldn't be reached.
                _errors.send(R.string.load_notifications_failed)
            } else {
                _state.value = fresh
            }
            _refreshing.value = false
        }
    }

    // "Mark as read": the dots go away at once, and the server remembers it for next time.
    fun markAllRead() {
        markLocally { true }
        tellServer { notifications.markAllRead() }
    }

    // A tap on a card, or an answer to its request. Nothing to do when it's read already.
    fun markRead(id: String) {
        val item = (_state.value as? LoadState.Ready)?.data?.items?.find { it.id == id } ?: return
        if (item.read) return
        markLocally { it.id == id }
        tellServer { notifications.markRead(id) }
    }

    private fun markLocally(which: (NotificationItem) -> Boolean) {
        _state.update { state ->
            if (state !is LoadState.Ready) return@update state
            val items = state.data.items.map { if (which(it)) it.copy(read = true) else it }
            state.copy(data = state.data.copy(items = items))
        }
    }

    private fun tellServer(call: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                call()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Not worth a message: they're read on this screen, and next time the server shows them again.
                Log.w("BookWorms", "Marking notifications as read failed", e)
            }
        }
    }

    // item = the "wants to borrow" notification; its referenceId is the request.
    fun accept(item: NotificationItem) = answer(item, accepted = true) { loans.accept(item.referenceId) }

    fun reject(item: NotificationItem) = answer(item, accepted = false) { loans.reject(item.referenceId) }

    private fun answer(item: NotificationItem, accepted: Boolean, call: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                call()
                _answers.update { it + (item.referenceId to accepted) }
                // Answered = seen. Told to the server before the list below loads again, so the dot doesn't come back.
                if (!item.read) {
                    markLocally { it.id == item.id }
                    try {
                        notifications.markRead(item.id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("BookWorms", "Marking a notification as read failed", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CannotAnswerException) {
                // Answered on another phone, taken back by the person who asked, or the book is lent out already.
                _errors.send(R.string.error_request_changed)
            } catch (e: Exception) {
                Log.w("BookWorms", "Answering a request from Notifications failed", e)
                _errors.send(R.string.error_save_failed)
            }
            // Accepting one request can change whether the others for the same book can be accepted.
            val fresh = load { fetch() }
            if (fresh is LoadState.Ready) _state.value = fresh
            _saving.value = false
        }
    }

    // Both lists at the same time.
    private suspend fun fetch(): NotificationsData = coroutineScope {
        val items = async { notifications.notifications() }
        val open = async { loans.receivedRequests() }
        NotificationsData(items.await(), open.await().associateBy { it.id })
    }
}
