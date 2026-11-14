package com.bookworms.server.loans.dto

import com.bookworms.server.copies.Availability
import java.time.Instant
import java.util.UUID

// "Who has it" on Book details: other people's copies of one book in my groups.
data class BookCopiesResponse(
	// All my groups, the ones I joined first at the top – also those where nobody has the book.
	val groups: List<GroupNameResponse>,
	val copies: List<OthersCopyResponse>,
)

data class GroupNameResponse(
	val id: UUID,
	val name: String,
)

data class OthersCopyResponse(
	val copyId: UUID,
	val owner: PersonResponse,
	// Only my groups – a group I'm not in is none of my business.
	val groupIds: List<UUID>,
	// Set by the owner: AVAILABLE or UNAVAILABLE (lost copies aren't sent). A lent-out copy is still AVAILABLE
	// here – see borrowedUntil.
	val availability: Availability,
	// When the owner added it.
	val addedAt: Instant,
	// When it's due back, or null when nobody has it now.
	val borrowedUntil: Instant?,
	// I'm the one who has it now.
	val borrowedByMe: Boolean,
	// My request waiting for the owner's answer, or null. Its id cancels it (POST /requests/{id}/cancel).
	val myRequestId: UUID?,
	// My place in the queue (1 = next), or null when I'm not waiting.
	val myQueuePosition: Long?,
	// How many people wait for it, me included.
	val queueLength: Long,
)
