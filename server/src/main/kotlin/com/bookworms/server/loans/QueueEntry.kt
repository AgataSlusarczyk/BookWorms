package com.bookworms.server.loans

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

// "Let me know when it's available": one person waiting for one copy.
// When the copy is available again, everyone waiting is told and the queue is cleared.
@Entity
@Table(name = "availability_watchers")
class QueueEntry(
	@EmbeddedId
	val id: QueueEntryId,
) {
	// Who came first – the place in the queue.
	val createdAt: Instant = Instant.now()
}

// The table has no id of its own: a person and a copy together are the key, so nobody is in one queue twice.
@Embeddable
data class QueueEntryId(
	val userId: UUID,
	val copyId: UUID,
) : Serializable
