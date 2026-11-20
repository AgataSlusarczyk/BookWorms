package com.bookworms.server.chat.dto

import com.bookworms.server.loans.dto.PersonResponse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// What the app sends from the chat's text field.
data class SendMessageRequest(
	@field:NotBlank
	@field:Size(max = 2000)
	val content: String,
)

// One message – in a page of the chat, as the answer to sending it, and on the live stream.
data class GroupMessageResponse(
	val id: UUID,
	val groupId: UUID,
	val sender: PersonResponse,
	val content: String,
	val createdAt: Instant,
)
