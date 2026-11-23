package com.bookworms.server.friends.dto

import java.util.UUID

// Who to send a friend request to. By id, from search, so nobody needs to know anyone's email.
data class SendFriendRequest(
	val userId: UUID,
)

// A friend, or someone found by search. No email: anyone could search, so it would show everyone's.
data class FriendResponse(
	val id: UUID,
	val name: String,
	val username: String,
	// A path on the server, e.g. "/photos/<file>.jpg". null = no photo.
	val photoUrl: String?,
)

data class FriendRequestResponse(
	val id: UUID,
	val requesterId: UUID,
	val requesterName: String,
	val requesterUsername: String,
	val requesterPhotoUrl: String?,
)

// One of a friend's books, on their profile.
data class FriendBookResponse(
	val copyId: UUID,
	val isbn: String?,
	val title: String,
	val authors: String?,
	val publishedYear: Short?,
	// A path on the server, e.g. "/covers/<file>.jpg". null = no cover.
	val coverUrl: String?,
)
