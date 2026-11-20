package com.bookworms.server.friends

import com.bookworms.server.common.ApiException
import com.bookworms.server.friends.dto.FriendRequestResponse
import com.bookworms.server.friends.dto.FriendResponse
import com.bookworms.server.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Friends: send a request by email, accept or reject incoming ones, list and remove friends.
@Service
class FriendService(
	private val friendshipRepository: FriendshipRepository,
	private val userRepository: UserRepository,
) {

	@Transactional(readOnly = true)
	fun listFriends(userId: UUID): List<FriendResponse> {
		val friendships = friendshipRepository.findAllForUserWithStatus(userId, FriendshipStatus.ACCEPTED)
		if (friendships.isEmpty()) return emptyList()
		val friendIds = friendships.map { if (it.requesterId == userId) it.addresseeId else it.requesterId }
		val users = userRepository.findAllById(friendIds.toSet()).associateBy { it.id!! }
		return friendIds.mapNotNull { id ->
			val user = users[id] ?: return@mapNotNull null
			FriendResponse(id = user.id!!, name = user.name, email = user.email)
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
				requesterEmail = user.email,
			)
		}
	}

	// Emails are matched in lower case. A previously rejected request can be sent again.
	@Transactional
	fun sendRequest(requesterId: UUID, targetEmail: String) {
		val target = userRepository.findByEmail(targetEmail.trim().lowercase())
			?: throw ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found")
		val addresseeId = target.id!!
		if (addresseeId == requesterId) {
			throw ApiException(HttpStatus.BAD_REQUEST, "CANNOT_FRIEND_SELF", "You can't add yourself as a friend")
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