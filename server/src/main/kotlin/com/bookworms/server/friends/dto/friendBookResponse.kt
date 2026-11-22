package com.bookworms.server.friends.dto

import java.util.UUID

data class FriendBookResponse(
	val copyId: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val publishedYear: Short?,
	val coverUrl: String?,
	val bookDescription: String?,
	val ownerNote: String?,
)