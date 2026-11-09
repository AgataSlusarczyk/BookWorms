package com.bookworms.server.loans

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface QueueRepository : JpaRepository<QueueEntry, QueueEntryId> {

	// "Id_CopyId" means id.copyId – the copy part of the key.

	// The queues I'm in, the oldest first.
	fun findById_UserIdOrderByCreatedAtAsc(userId: UUID): List<QueueEntry>

	// My place = how many joined this copy's queue before me, plus me.
	fun countById_CopyIdAndCreatedAtLessThanEqual(copyId: UUID, createdAt: Instant): Long

	// The copy is available again – the queue is no longer needed.
	fun deleteById_CopyId(copyId: UUID)
}
