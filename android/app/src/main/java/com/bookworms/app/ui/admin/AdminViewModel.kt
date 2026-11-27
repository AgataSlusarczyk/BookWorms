package com.bookworms.app.ui.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.AdminRepository
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.CannotBlockException
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.load
import com.bookworms.app.ui.browse.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The admin panel: numbers, accounts, groups, the catalog and what the admins did.
// Each tab loads when it's opened, and again when the user comes back to it.
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: AdminRepository = AdminRepository(),
    private val catalog: BookRepository = BookRepository(),
) : ViewModel() {

    private val _stats = MutableStateFlow<LoadState<AdminStats>>(LoadState.Loading)
    val stats: StateFlow<LoadState<AdminStats>> = _stats.asStateFlow()

    private val _groups = MutableStateFlow<LoadState<List<AdminGroup>>>(LoadState.Loading)
    val groups: StateFlow<LoadState<List<AdminGroup>>> = _groups.asStateFlow()

    private val _logs = MutableStateFlow<LoadState<List<AdminLogEntry>>>(LoadState.Loading)
    val logs: StateFlow<LoadState<List<AdminLogEntry>>> = _logs.asStateFlow()

    private val _userQuery = MutableStateFlow("")
    val userQuery: StateFlow<String> = _userQuery.asStateFlow()

    private val _bookQuery = MutableStateFlow("")
    val bookQuery: StateFlow<String> = _bookQuery.asStateFlow()

    // Go up on "Try again" (and after a change), so the same query is sent once more.
    private val userRetries = MutableStateFlow(0)
    private val bookRetries = MutableStateFlow(0)

    // Waits until the admin stops typing for a moment, like Browse. An empty query = the newest accounts.
    val users: StateFlow<LoadState<List<AdminUser>>> =
        combine(_userQuery.debounce { if (it.isBlank()) 0L else SearchDelayMillis }, userRetries) { query, _ -> query }
            .transformLatest { query ->
                emit(LoadState.Loading)
                emit(load { repository.users(query) })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    // The catalog, like Browse. A tap opens Book details, where the admin edits or deletes the book.
    val books: StateFlow<LoadState<List<Book>>> =
        combine(_bookQuery.debounce { if (it.isBlank()) 0L else SearchDelayMillis }, bookRetries) { query, _ -> query }
            .transformLatest { query ->
                emit(LoadState.Loading)
                emit(load { catalog.search(query) })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    // The account or group whose button was just tapped, while the server answers. Its button waits until then.
    private val _busyId = MutableStateFlow<String?>(null)
    val busyId: StateFlow<String?> = _busyId.asStateFlow()

    // Short messages (Toast) as string resources, each shown once.
    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    // Each time a tab shows up – something may have changed on another screen (e.g. a book deleted on Book details).
    fun refresh(tab: AdminTab) {
        when (tab) {
            AdminTab.Overview -> _stats.reload { repository.stats() }
            AdminTab.Users -> userRetries.value++
            AdminTab.Groups -> _groups.reload { repository.groups() }
            AdminTab.Books -> bookRetries.value++
            AdminTab.Log -> _logs.reload { repository.logs() }
        }
    }

    fun onUserQueryChange(query: String) {
        _userQuery.value = query
    }

    fun onBookQueryChange(query: String) {
        _bookQuery.value = query
    }

    fun setBlocked(user: AdminUser, blocked: Boolean) = change(user.id) {
        repository.setBlocked(user.id, blocked)
        userRetries.value++
        if (blocked) R.string.admin_user_blocked else R.string.admin_user_unblocked
    }

    fun deleteGroup(group: AdminGroup) = change(group.id) {
        repository.deleteGroup(group.id)
        // Gone from the list right away; the server's list comes after.
        _groups.update { state ->
            if (state is LoadState.Ready) LoadState.Ready(state.data.filter { it.id != group.id }) else state
        }
        R.string.admin_group_deleted
    }

    // One change at a time. call gives the message to show when it went through.
    private fun change(id: String, call: suspend () -> Int) {
        if (_busyId.value != null) return
        _busyId.value = id
        viewModelScope.launch {
            val message = try {
                call()
            } catch (e: CancellationException) {
                throw e
            } catch (e: CannotBlockException) {
                R.string.admin_cannot_block
            } catch (e: Exception) {
                Log.w("BookWorms", "An admin change failed", e)
                R.string.admin_action_failed
            }
            _busyId.value = null
            _messages.send(message)
        }
    }

    // What's on the screen stays while the fresh data loads; "Try again" only when there was nothing to show.
    private fun <T> MutableStateFlow<LoadState<T>>.reload(block: suspend () -> T) {
        if (value !is LoadState.Ready) value = LoadState.Loading
        viewModelScope.launch {
            val fresh = load(block)
            if (fresh is LoadState.Ready || value !is LoadState.Ready) value = fresh
        }
    }

    private companion object {
        const val SearchDelayMillis = 300L
    }
}
