package com.bookworms.app.data

import com.bookworms.app.BuildConfig
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.GroupMessageDto
import com.bookworms.app.data.network.GroupsApi
import com.bookworms.app.data.network.SendMessageDto
import com.bookworms.app.ui.groups.ChatMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.time.Instant

// What the live stream of a group's chat brings.
internal sealed interface ChatUpdate {
    // The stream is open. Messages sent while it was closed aren't on it – ask for the newest page.
    data object Connected : ChatUpdate

    data class NewMessage(val message: ChatMessage) : ChatUpdate
}

// The stream couldn't be opened or broke. code = the server's answer, null when it couldn't be reached.
internal class ChatStreamException(val code: Int?, cause: Throwable?) : Exception("Chat stream failed ($code)", cause)

// A group's chat: pages of older messages, sending, and new messages live.
// Every function throws when the server can't be reached or answers with an error.
internal class ChatRepository(
    private val api: GroupsApi = ApiClient.groups,
    private val streams: OkHttpClient = ApiClient.streams,
) {

    suspend fun newest(groupId: String): List<ChatMessage> = api.messages(groupId, limit = PageSize).map { it.toMessage() }

    suspend fun older(groupId: String, before: String): List<ChatMessage> =
        api.messages(groupId, before = before, limit = PageSize).map { it.toMessage() }

    suspend fun send(groupId: String, text: String): ChatMessage = api.sendMessage(groupId, SendMessageDto(text)).toMessage()

    // Open while it's collected. Ends when the server ends the stream (every 30 minutes),
    // and fails with ChatStreamException when it breaks – e.g. 404 when the user is no longer in the group.
    fun live(groupId: String): Flow<ChatUpdate> = callbackFlow {
        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "groups/$groupId/messages/stream")
            // JSON too, so an error comes with the server's explanation.
            .header("Accept", "text/event-stream, application/json")
            .build()
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                trySend(ChatUpdate.Connected)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type != "message") return
                val message = try {
                    ApiClient.json.decodeFromString<GroupMessageDto>(data).toMessage()
                } catch (e: Exception) {
                    // Something a newer server sends that this app doesn't understand – skipped.
                    return
                }
                trySend(ChatUpdate.NewMessage(message))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(ChatStreamException(response?.code, t))
            }
        }
        val source = EventSources.createFactory(streams).newEventSource(request, listener)
        awaitClose { source.cancel() }
    }
        // Nothing gets lost while the screen is busy, e.g. loading the newest page after Connected.
        .buffer(Channel.UNLIMITED)

    companion object {
        const val PageSize = 50
    }
}

private fun GroupMessageDto.toMessage() = ChatMessage(
    id = id,
    senderId = sender.id,
    senderName = sender.name,
    text = content,
    sentAt = Instant.parse(createdAt),
)
