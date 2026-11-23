package com.bookworms.server.friends

import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.copies.CopyGroupVisibilityRepository
import com.bookworms.server.friends.dto.FriendBookResponse
import com.bookworms.server.friends.dto.FriendRequestResponse
import com.bookworms.server.friends.dto.FriendResponse
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
import com.bookworms.server.users.searchByUsername
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Friends: search users, send a request, accept or reject incoming ones, list friends and their books.
@Service
class FriendService(
	private val friendshipRepository: FriendshipRepository,
	private val userRepository: UserRepository,
	private val copyRepository: BookCopyRepository,
	private val visibilityRepository: CopyGroupVisibilityRepository,
) {

	@Transactional(readOnly = true)
	fun listFriends(userId: UUID): List<FriendResponse> {
		val friendships = friendshipRepository.findAllForUserWithStatus(userId, FriendshipStatus.ACCEPTED)
		if (friendships.isEmpty()) return emptyList()
		val friendIds = friendships.map { if (it.requesterId == userId) it.addresseeId else it.requesterId }
		val users = userRepository.findAllById(friendIds.toSet()).associateBy { it.id!! }
		return friendIds.mapNotNull { id -> users[id]?.toFriend() }
	}

	// The friend's copies that are shown in at least one of their groups. A copy hidden from every group
	// is hidden from friends too, and a lost one isn't there. The owner's own description stays private.
	@Transactional(readOnly = true)
	fun listFriendBooks(userId: UUID, friendId: UUID): List<FriendBookResponse> {
		friendshipRepository.findBetween(userId, friendId)
			?.takeIf { it.status == FriendshipStatus.ACCEPTED }
			?: throw ApiException(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "This person isn't your friend")

		val copies = copyRepository.findByOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(friendId)
			.filter { it.availability != Availability.LOST }
		val shown = visibilityRepository.findById_CopyIdIn(copies.map { it.id!! }).map { it.id.copyId }.toSet()
		return copies.filter { it.id in shown }.map { copy ->
			FriendBookResponse(
				copyId = copy.id!!,
				isbn = copy.book.isbn,
				title = copy.book.title,
				authors = copy.book.authors,
				publishedYear = copy.book.publishedYear,
				coverUrl = copy.book.coverUrl,
			)
		}
	}

	@Transactional(readOnly = true)
	fun listIncomingRequests(userId: UUID): List<FriendRequestResponse> {
		val requests = friendshipRepository.findByAddresseeIdAndStatusOrderByCreatedAtDesc(
			userId,
			FriendshipStatus.PENDING,
		)
		if (requests.isEmpty()) return emptyList()
		val requesters = userRepository.findAllById(requests.map { it.requesterId }.toSet()).associateBy { it.id!! }
		return requests.mapNotNull { request ->
			val user = requesters[request.requesterId] ?: return@mapNotNull null
			FriendRequestResponse(
				id = request.id!!,
				requesterId = user.id!!,
				requesterName = user.name,
				requesterUsername = user.username,
				requesterPhotoUrl = user.photoUrl,
			)
		}
	}

	@Transactional(readOnly = true)
	fun searchUsers(userId: UUID, query: String): List<FriendResponse> {
		val trimmed = query.trim()
		if (trimmed.length < 2) return emptyList()

		val excludedIds = buildSet {
			add(userId)
			friendshipRepository.findAllForUserWithStatus(userId, FriendshipStatus.ACCEPTED).forEach {
				add(it.requesterId)
				add(it.addresseeId)
			}
			friendshipRepository.findAllForUserWithStatus(userId, FriendshipStatus.PENDING).forEach {
				add(it.requesterId)
				add(it.addresseeId)
			}
		}

		return userRepository.searchByUsername(trimmed)
			.filter { it.id !in excludedIds }
			.take(20)
			.map { it.toFriend() }
	}

	// A previously rejected request can be sent again.
	@Transactional
	fun sendRequest(requesterId: UUID, addresseeId: UUID) {
		if (addresseeId == requesterId) {
			throw ApiException(HttpStatus.BAD_REQUEST, "CANNOT_FRIEND_SELF", "You can't add yourself as a friend")
		}
		if (!userRepository.existsById(addresseeId)) {
			throw ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found")
		}

		val existing = friendshipRepository.findBetween(requesterId, addresseeId)
		if (existing != null) {
			when (existing.status) {
				FriendshipStatus.ACCEPTED ->
					throw ApiException(HttpStatus.CONFLICT, "ALREADY_FRIENDS", "You are already friends")
				FriendshipStatus.PENDING ->
					throw ApiException(HttpStatus.CONFLICT, "FRIEND_REQUEST_EXISTS", "Friend request already pending")
				FriendshipStatus.REJECTED -> {
					friendshipRepository.delete(existing)
					friendshipRepository.flush()
				}
			}
		}

		friendshipRepository.save(Friendship(requesterId = requesterId, addresseeId = addresseeId))
	}

	@Transactional
	fun acceptRequest(userId: UUID, requestId: UUID) {
		val request = pendingIncomingRequest(userId, requestId)
		request.status = FriendshipStatus.ACCEPTED
	}

	@Transactional
	fun rejectRequest(userId: UUID, requestId: UUID) {
		val request = pendingIncomingRequest(userId, requestId)
		request.status = FriendshipStatus.REJECTED
	}

	@Transactional
	fun removeFriend(userId: UUID, friendId: UUID) {
		val friendship = friendshipRepository.findBetween(userId, friendId)
			?.takeIf { it.status == FriendshipStatus.ACCEPTED }
			?: throw ApiException(HttpStatus.NOT_FOUND, "FRIEND_NOT_FOUND", "This person isn't your friend")
		friendshipRepository.delete(friendship)
	}

	private fun pendingIncomingRequest(userId: UUID, requestId: UUID): Friendship =
		friendshipRepository.findById(requestId)
			.orElseThrow { ApiException(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND", "Friend request not found") }
			.takeIf { it.addresseeId == userId && it.status == FriendshipStatus.PENDING }
			?: throw ApiException(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND", "Friend request not found")
}

// What others may see about a user: no email.
internal fun User.toFriend() = FriendResponse(id = id!!, name = name, username = username, photoUrl = photoUrl)
