package com.bookworms.server.chat

import com.bookworms.server.chat.dto.GroupMessageResponse
import com.bookworms.server.groups.GroupDeleted
import com.bookworms.server.groups.MemberLeftGroup
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// A new message sent – told after it's saved, so nobody gets a message that's then rolled back.
data class GroupMessageSent(val message: GroupMessageResponse)

// The open chats: every phone with a group's chat on screen keeps one connection (Server-Sent Events),
// and each new message goes to all of them right away.
// Kept in memory, so it works with one server. With more of them, they'd have to pass messages on to each other.
@Component
class ChatHub {

	private class Stream(val groupId: UUID, val userId: UUID, val emitter: SseEmitter)

	// Group id → the streams open for it.
	private val streams = ConcurrentHashMap<UUID, MutableSet<Stream>>()

	fun subscribe(groupId: UUID, userId: UUID): SseEmitter {
		val emitter = SseEmitter(StreamTimeout.toMillis())
		val stream = Stream(groupId, userId, emitter)
		streams.computeIfAbsent(groupId) { ConcurrentHashMap.newKeySet() }.add(stream)
		emitter.onCompletion { remove(stream) }
		emitter.onError { remove(stream) }
		// After StreamTimeout the stream ends and the app opens a new one.
		emitter.onTimeout { emitter.complete() }
		// Sent right away, so the answer starts now and the app knows it's connected.
		send(stream, SseEmitter.event().comment("connected"))
		return emitter
	}

	// The same stream, given back before anyone saw it (the person turned out not to be in the group).
	fun unsubscribe(groupId: UUID, emitter: SseEmitter) {
		streams[groupId]?.filter { it.emitter === emitter }?.forEach { close(it) }
	}

	@TransactionalEventListener
	fun on(event: GroupMessageSent) {
		val message = event.message
		val sse = SseEmitter.event()
			.name("message")
			.id(message.id.toString())
			.data(message, MediaType.APPLICATION_JSON)
		streams[message.groupId]?.forEach { send(it, sse) }
	}

	// Someone outside the group mustn't hear what's said in it anymore.
	@TransactionalEventListener
	fun on(event: MemberLeftGroup) {
		streams[event.groupId]?.filter { it.userId == event.userId }?.forEach { close(it) }
	}

	@TransactionalEventListener
	fun on(event: GroupDeleted) {
		streams[event.groupId]?.forEach { close(it) }
	}

	// A comment every now and then. Without it a quiet chat looks like a dead connection to the phone
	// and to anything between them – and it's how we notice phones that left without saying goodbye.
	@Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
	fun keepAlive() {
		streams.values.flatten().forEach { send(it, SseEmitter.event().comment("ping")) }
	}

	private fun send(stream: Stream, event: SseEmitter.SseEventBuilder) {
		try {
			stream.emitter.send(event)
		} catch (e: IOException) {
			// The phone went away. The server finishes the connection on its own.
			remove(stream)
		} catch (e: IllegalStateException) {
			// Finished already, e.g. it timed out a moment ago.
			remove(stream)
		}
	}

	private fun close(stream: Stream) {
		remove(stream)
		stream.emitter.complete()
	}

	private fun remove(stream: Stream) {
		streams.computeIfPresent(stream.groupId) { _, set ->
			set.remove(stream)
			set.ifEmpty { null }
		}
	}

	private companion object {
		val StreamTimeout: Duration = Duration.ofMinutes(30)
	}
}
