package com.bookworms.server.friends

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface FriendshipRepository : JpaRepository<Friendship, UUID> {

	// Accepted friendships (or requests) where the user is on either side.
	@Query(
		"""
		select f from Friendship f
		where f.status = :status
		  and (f.requesterId = :userId or f.addresseeId = :userId)
		order by f.createdAt desc
		""",
	)
	fun findAllForUserWithStatus(userId: UUID, status: FriendshipStatus): List<Friendship>

	// Incoming requests waiting for this user's decision, newest first.
	fun findByAddresseeIdAndStatusOrderByCreatedAtDesc(
		addresseeId: UUID,
		status: FriendshipStatus,
	): List<Friendship>

	// Any existing row between two people, regardless of who sent the request.
	@Query(
		"""
		select f from Friendship f
		where (f.requesterId = :first and f.addresseeId = :second)
		   or (f.requesterId = :second and f.addresseeId = :first)
		""",
	)
	fun findBetween(first: UUID, second: UUID): Friendship?
}
