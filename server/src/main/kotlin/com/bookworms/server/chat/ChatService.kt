package com.bookworms.server.chat

import com.bookworms.server.chat.dto.GroupMessageResponse
import com.bookworms.server.chat.dto.SendMessageRequest
import com.bookworms.server.common.ApiException
import com.bookworms.server.groups.GroupMemberId
import com.bookworms.server.groups.GroupMemberRepository
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.users.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

// The group's chat: read it page by page, write to it, and hear new messages live (see ChatHub).
// Like the rest of a group, someone outside it gets "not found".
@Service
class ChatService(
	private val messageRepository: GroupMessageRepository,
	private val memberRepository: GroupMemberRepository,
	private val userRepository: UserRepository,
	private val hub: ChatHub,
	private val events: ApplicationEventPublisher,
) {

	// The newest first. before = the oldest message the app has, for the page before it.
	@Transactional(readOnly = true)
	fun messages(userId: UUID, groupId: UUID, before: UUID?, limit: Int): List<GroupMessageResponse> {
		requireMember(userId, groupId)
		val size = limit.coerceIn(1, MaxPageSize)
		val page = if (before == null) {
			messageRepository.findNewest(groupId, size)
		} else {
			val oldest = messageRepository.findById(before).filter { it.groupId == groupId }.orElseThrow {
				ApiException(HttpStatus.NOT_FOUND, "MESSAGE_NOT_FOUND", "Message not found")
			}
			messageRepository.findOlder(groupId, oldest.createdAt, oldest.id!!, size)
		}
		return describe(page)
	}

	@Transactional
	fun send(userId: UUID, groupId: UUID, request: SendMessageRequest): GroupMessageResponse {
		requireMember(userId, groupId)
		val message = messageRepository.save(GroupMessage(groupId = groupId, senderId = userId, content = request.content.trim()))
		val response = describe(listOf(message)).single()
		events.publishEvent(GroupMessageSent(response))
		return response
	}

	// Listed first and checked after, so leaving the group at the same moment can't slip between the two:
	// either the check already sees them gone, or they were listed in time for ChatHub to close it.
	fun stream(userId: UUID, groupId: UUID): SseEmitter {
		val emitter = hub.subscribe(groupId, userId)
		if (!memberRepository.existsById(GroupMemberId(userId, groupId))) {
			hub.unsubscribe(groupId, emitter)
			throw groupNotFound()
		}
		return emitter
	}

	private fun requireMember(userId: UUID, groupId: UUID) {
		if (!memberRepository.existsById(GroupMemberId(userId, groupId))) throw groupNotFound()
	}

	private fun groupNotFound() = ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found")

	// Adds the senders' names – all of them in one query. Someone who left the group keeps their name.
	private fun describe(messages: List<GroupMessage>): List<GroupMessageResponse> {
		val names = userRepository.findAllById(messages.map { it.senderId }.toSet()).associate { it.id!! to it.name }
		return messages.map {
			GroupMessageResponse(
				id = it.id!!,
				groupId = it.groupId,
				sender = PersonResponse(it.senderId, names[it.senderId].orEmpty()),
				content = it.content,
				createdAt = it.createdAt,
			)
		}
	}

	private companion object {
		const val MaxPageSize = 100
	}
}
