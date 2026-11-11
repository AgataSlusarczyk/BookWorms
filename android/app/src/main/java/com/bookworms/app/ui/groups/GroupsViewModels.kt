package com.bookworms.app.ui.groups

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.GroupsRepository
import com.bookworms.app.data.InviteNotFoundException
import com.bookworms.app.data.LoadState
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

// The Groups tab.
internal class GroupsViewModel(private val repository: GroupsRepository = GroupsRepository()) : ViewModel() {

    private val _groups = MutableStateFlow<LoadState<List<GroupSummary>>>(LoadState.Loading)
    val groups: StateFlow<LoadState<List<GroupSummary>>> = _groups.asStateFlow()

    // Called each time the tab opens, so a group made, joined or left on another screen shows up.
    // The list already on screen stays while the fresh one loads.
    fun refresh() {
        if (_groups.value !is LoadState.Ready) _groups.value = LoadState.Loading
        viewModelScope.launch {
            val fresh = load { repository.myGroups() }
            if (fresh is LoadState.Ready || _groups.value !is LoadState.Ready) _groups.value = fresh
        }
    }
}

// What happened to something sent to the server. Each one is handled once.
internal sealed interface GroupEvent {
    // Create group or Join group: open this group.
    data class Opened(val groupId: String) : GroupEvent

    // Edit group was saved – back to Group details.
    data object Saved : GroupEvent

    // Left or deleted – back to the Groups list.
    data object Gone : GroupEvent

    // A short message (Toast).
    data class Failed(@StringRes val message: Int) : GroupEvent
}

// Runs a change, one at a time, and turns an error into a message.
internal abstract class SavingViewModel : ViewModel() {

    // true while a change is on its way – the buttons wait for it.
    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = Channel<GroupEvent>(Channel.BUFFERED)
    val events: Flow<GroupEvent> = _events.receiveAsFlow()

    protected suspend fun send(event: GroupEvent) = _events.send(event)

    protected fun save(onError: (Exception) -> Int = { R.string.error_save_failed }, block: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Saving a group change failed", e)
                _events.send(GroupEvent.Failed(onError(e)))
            } finally {
                _saving.value = false
            }
        }
    }
}

// Create group.
internal class CreateGroupViewModel(private val repository: GroupsRepository = GroupsRepository()) : SavingViewModel() {

    fun create(name: String, description: String) = save {
        send(GroupEvent.Opened(repository.create(name, description).id))
    }
}

// Join group: type the code, see the group, join it.
internal class JoinGroupViewModel(private val repository: GroupsRepository = GroupsRepository()) : SavingViewModel() {

    // The group behind the typed code. null = nothing looked up yet.
    private val _found = MutableStateFlow<InvitePreview?>(null)
    val found: StateFlow<InvitePreview?> = _found.asStateFlow()

    // true = no group has the typed code.
    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    // A new code means a new search.
    fun onCodeChange() {
        _found.value = null
        _notFound.value = false
    }

    fun find(code: String) = save(onError = { R.string.error_server_unreachable }) {
        try {
            _found.value = repository.preview(code)
        } catch (e: InviteNotFoundException) {
            _notFound.value = true
        }
    }

    fun join(code: String) = save(onError = { R.string.error_server_unreachable }) {
        try {
            send(GroupEvent.Opened(repository.join(code).id))
        } catch (e: InviteNotFoundException) {
            // The admin made a new code in the meantime.
            _found.value = null
            _notFound.value = true
        }
    }
}

// Group details, and Edit group and Invite opened from it – they share it (see AppNavigation),
// so a change made on one shows on the others.
internal class GroupDetailsViewModel(
    private val groupId: String,
    private val repository: GroupsRepository = GroupsRepository(),
) : SavingViewModel() {

    private val _group = MutableStateFlow<LoadState<GroupDetails>>(LoadState.Loading)
    val group: StateFlow<LoadState<GroupDetails>> = _group.asStateFlow()

    private val _books = MutableStateFlow<LoadState<List<GroupBook>>>(LoadState.Loading)
    val books: StateFlow<LoadState<List<GroupBook>>> = _books.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _group.value = LoadState.Loading
        _books.value = LoadState.Loading
        viewModelScope.launch { _group.value = load { repository.details(groupId) } }
        viewModelScope.launch { _books.value = load { repository.books(groupId) } }
    }

    fun update(name: String, description: String, membersCanInvite: Boolean) = save {
        _group.value = LoadState.Ready(repository.update(groupId, name, description, membersCanInvite))
        send(GroupEvent.Saved)
    }

    fun delete() = save {
        repository.delete(groupId)
        send(GroupEvent.Gone)
    }

    fun leave() = save {
        repository.leave(groupId)
        send(GroupEvent.Gone)
    }

    fun newInviteCode() = save {
        _group.value = LoadState.Ready(repository.newInviteCode(groupId))
    }

    fun makeAdmin(member: GroupMember) = save {
        _group.value = LoadState.Ready(repository.makeAdmin(groupId, member.id))
    }

    // The person's books leave the group with them, so the books are asked for again.
    fun removeMember(member: GroupMember) = save {
        _group.value = LoadState.Ready(repository.removeMember(groupId, member.id))
        _books.update { LoadState.Loading }
        _books.value = load { repository.books(groupId) }
    }
}
