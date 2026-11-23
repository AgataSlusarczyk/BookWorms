package com.bookworms.app.ui.friends

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.Friend
import com.bookworms.app.data.FriendBook
import com.bookworms.app.data.FriendsOverview
import com.bookworms.app.data.FriendsRepository
import com.bookworms.app.data.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal sealed interface FriendsEvent {
    data class Message(val text: String) : FriendsEvent
    data object FriendRemoved : FriendsEvent
}

internal class FriendsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = FriendsRepository(application.applicationContext)

    private val _overview = MutableStateFlow<LoadState<FriendsOverview>>(LoadState.Loading)
    val overview: StateFlow<LoadState<FriendsOverview>> = _overview.asStateFlow()

    private val _friendBooks = MutableStateFlow<LoadState<List<FriendBook>>>(LoadState.Loading)
    val friendBooks: StateFlow<LoadState<List<FriendBook>>> = _friendBooks.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Friend>>(emptyList())
    val searchResults: StateFlow<List<Friend>> = _searchResults.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = Channel<FriendsEvent>(Channel.BUFFERED)
    val events: Flow<FriendsEvent> = _events.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        _overview.value = LoadState.Loading
        viewModelScope.launch {
            try {
                _overview.value = LoadState.Ready(repository.getOverview())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Loading friends failed, checking offline cache", e)
                val cached = repository.getCachedOverview()
                _overview.value = if (cached != null) LoadState.Ready(cached) else LoadState.Failed
            }
        }
    }

    fun loadFriendBooks(friendId: String) {
        _friendBooks.value = LoadState.Loading
        viewModelScope.launch {
            try {
                _friendBooks.value = LoadState.Ready(repository.getFriendBooks(friendId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Loading friend books failed", e)
                _friendBooks.value = LoadState.Failed
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.trim().length < 2) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                _searchResults.value = repository.searchUsers(newQuery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Searching users failed", e)
            }
        }
    }

    fun sendRequest(userId: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.sendRequest(userId)
                _searchResults.value = _searchResults.value.filterNot { it.id == userId }
                _events.send(FriendsEvent.Message("Friend request sent"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Sending friend request failed", e)
                _events.send(FriendsEvent.Message("Could not send friend request"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun accept(requestId: String) = runAction { repository.acceptRequest(requestId) }
    fun reject(requestId: String) = runAction { repository.rejectRequest(requestId) }

    fun remove(userId: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.removeFriend(userId)
                _overview.value = LoadState.Ready(repository.getOverview())
                _events.send(FriendsEvent.FriendRemoved)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Removing friend failed", e)
                _events.send(FriendsEvent.Message("Could not remove friend"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun inviteToGroup(groupId: String, userId: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                repository.inviteToGroup(groupId, userId)
                _events.send(FriendsEvent.Message("Group invitation sent"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Group invitation failed", e)
                _events.send(FriendsEvent.Message("User is already in the group or invited"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun acceptGroupInvitation(invitationId: String) = runAction("Joined group") {
        repository.acceptGroupInvitation(invitationId)
    }

    fun rejectGroupInvitation(invitationId: String) = runAction("Invitation declined") {
        repository.rejectGroupInvitation(invitationId)
    }

    private fun runAction(successMessage: String? = null, block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
                _overview.value = LoadState.Ready(repository.getOverview())
                if (successMessage != null) _events.send(FriendsEvent.Message(successMessage))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Friend action failed", e)
                _events.send(FriendsEvent.Message("Action failed"))
            } finally {
                _busy.value = false
            }
        }
    }
}
