package com.bookworms.app.ui.groups

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.R
import com.bookworms.app.data.ChatRepository
import com.bookworms.app.data.ChatStreamException
import com.bookworms.app.data.ChatUpdate
import com.bookworms.app.data.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The Chat tab of Group details. New messages come live while the tab is on screen (stayConnected).
internal class ChatViewModel(
    private val groupId: String,
    private val repository: ChatRepository = ChatRepository(),
) : ViewModel() {

    // The newest first – the list is drawn from the bottom up.
    private val _messages = MutableStateFlow<LoadState<List<ChatMessage>>>(LoadState.Loading)
    val messages: StateFlow<LoadState<List<ChatMessage>>> = _messages.asStateFlow()

    // false = the live stream is (re)connecting, so new messages may come late.
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _loadingOlder = MutableStateFlow(false)
    val loadingOlder: StateFlow<Boolean> = _loadingOlder.asStateFlow()

    // true while a message is on its way – the Send button waits for it.
    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    // What's typed in the text field. Compose state, so typing stays smooth.
    var draft by mutableStateOf("")
        private set

    private val _events = Channel<GroupEvent>(Channel.BUFFERED)
    val events: Flow<GroupEvent> = _events.receiveAsFlow()

    // true = the chat's first message is on screen, there's nothing older.
    private var reachedStart = false

    init {
        refresh()
    }

    fun refresh() {
        if (_messages.value !is LoadState.Ready) _messages.value = LoadState.Loading
        viewModelScope.launch {
            try {
                loadNewest()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Loading the chat failed", e)
                if (_messages.value !is LoadState.Ready) _messages.value = LoadState.Failed
            }
        }
    }

    // Keeps the live stream open until it's cancelled (the tab closes or the app goes to the background).
    // When it breaks, it opens it again – soon at first, then less and less often, so a server that's off
    // isn't asked every second.
    suspend fun stayConnected() {
        var wait = FirstRetry
        while (true) {
            try {
                repository.live(groupId).collect { update ->
                    when (update) {
                        ChatUpdate.Connected -> {
                            _connected.value = true
                            wait = FirstRetry
                            catchUp()
                        }
                        is ChatUpdate.NewMessage -> add(listOf(update.message))
                    }
                }
                // The server ended the stream (it does it now and then) – a new one opens right away.
            } catch (e: ChatStreamException) {
                _connected.value = false
                when (e.code) {
                    // The user was removed from the group, or it was deleted.
                    404 -> {
                        _events.send(GroupEvent.Gone)
                        return
                    }
                    // Logged out (ApiClient took the app back to the login screen).
                    401 -> return
                }
                Log.w("BookWorms", "The chat's live stream broke", e)
                delay(wait)
                wait = (wait * 2).coerceAtMost(MaxRetry)
            } finally {
                _connected.value = false
            }
        }
    }

    fun loadOlder() {
        val shown = (_messages.value as? LoadState.Ready)?.data ?: return
        if (reachedStart || _loadingOlder.value || shown.isEmpty()) return
        _loadingOlder.value = true
        viewModelScope.launch {
            try {
                val page = repository.older(groupId, before = shown.last().id)
                if (page.size < ChatRepository.PageSize) reachedStart = true
                add(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Scrolling up again tries once more.
                Log.w("BookWorms", "Loading older messages failed", e)
            } finally {
                _loadingOlder.value = false
            }
        }
    }

    fun onDraftChange(text: String) {
        draft = text.take(MaxMessageLength)
    }

    // The field empties right away, so the next message can be typed. If sending fails, the text comes back.
    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || _sending.value) return
        _sending.value = true
        draft = ""
        viewModelScope.launch {
            try {
                add(listOf(repository.send(groupId, text)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Sending a message failed", e)
                if (draft.isEmpty()) draft = text
                _events.send(GroupEvent.Failed(R.string.error_send_failed))
            } finally {
                _sending.value = false
            }
        }
    }

    // Messages sent while the stream was closed. Not being able to get them isn't worth an error:
    // the live ones still come, and the next connection tries again.
    private suspend fun catchUp() {
        try {
            loadNewest()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("BookWorms", "Catching up on the chat failed", e)
        }
    }

    private suspend fun loadNewest() {
        val page = repository.newest(groupId)
        _messages.update { shown ->
            val old = (shown as? LoadState.Ready)?.data
            when {
                old.isNullOrEmpty() -> {
                    reachedStart = page.size < ChatRepository.PageSize
                    LoadState.Ready(page)
                }
                // A long break: more was said than one page holds, so the messages in between are missing.
                // Starting again from the newest page – the older ones load when scrolling up.
                page.size == ChatRepository.PageSize && page.last().sentAt > old.first().sentAt -> {
                    reachedStart = false
                    LoadState.Ready(page)
                }
                else -> LoadState.Ready(merge(old, page))
            }
        }
    }

    // Live, sent or older ones. The same message can come twice (e.g. the answer to sending it
    // and the live stream), so they're matched by id.
    private fun add(messages: List<ChatMessage>) {
        _messages.update { shown -> if (shown is LoadState.Ready) LoadState.Ready(merge(shown.data, messages)) else shown }
    }

    private fun merge(shown: List<ChatMessage>, more: List<ChatMessage>): List<ChatMessage> =
        (more + shown).distinctBy { it.id }.sortedWith(NewestFirst)

    private companion object {
        // The same order as the server's: by time, then by id when two have the same time.
        val NewestFirst = compareByDescending<ChatMessage> { it.sentAt }.thenByDescending { it.id }
        const val FirstRetry = 2_000L
        const val MaxRetry = 30_000L
        const val MaxMessageLength = 2000
    }
}
