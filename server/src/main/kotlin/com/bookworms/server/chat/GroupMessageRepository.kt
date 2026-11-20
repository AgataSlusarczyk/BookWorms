package com.bookworms.server.chat

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

// Pages of a chat, the newest first. Two messages can have the same time, so the id breaks the tie –
// that way a page never skips or repeats a message.
interface GroupMessageRepository : JpaRepository<GroupMessage, UUID> {

	@Query(
		value = """
		select * from group_messages
		where group_id = :groupId
		order by created_at desc, id desc
		limit :limit
		""",
		nativeQuery = true,
	)
	fun findNewest(groupId: UUID, limit: Int): List<GroupMessage>

	// The page right before (older than) the given message.
	@Query(
		value = """
		select * from group_messages
		where group_id = :groupId and (created_at, id) < (:createdAt, :id)
		order by created_at desc, id desc
		limit :limit
		""",
		nativeQuery = true,
	)
	fun findOlder(groupId: UUID, createdAt: Instant, id: UUID, limit: Int): List<GroupMessage>
}
