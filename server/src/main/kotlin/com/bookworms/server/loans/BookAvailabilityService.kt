package com.bookworms.server.loans

import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.copies.CopyGroupVisibilityRepository
import com.bookworms.server.groups.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Can the user borrow the book from someone in their groups?
// AVAILABLE = at least one copy can be asked for right now.
// BORROWED = none can, but at least one is lent out, so it comes back ("Notify me").
enum class BookAvailability { AVAILABLE, BORROWED }

// The tag next to a book on the user's lists and on Want to read.
// Only copies shown in the user's groups count – not the user's own, not removed, not lost.
// A book nobody in the groups has, or only has as "not available", isn't in the answer at all.
@Service
class BookAvailabilityService(
	private val copyRepository: BookCopyRepository,
	private val visibilityRepository: CopyGroupVisibilityRepository,
	private val memberRepository: GroupMemberRepository,
	private val borrowingRepository: BorrowingRepository,
) {

	@Transactional(readOnly = true)
	fun forBooks(userId: UUID, bookIds: Collection<UUID>): Map<UUID, BookAvailability> {
		if (bookIds.isEmpty()) return emptyMap()
		val myGroups = memberRepository.findById_UserIdOrderByJoinedAtAsc(userId).map { it.id.groupId }.toSet()
		if (myGroups.isEmpty()) return emptyMap()

		val allCopies = copyRepository.findByBook_IdInAndOwnerIdNotAndDeletedAtIsNull(bookIds, userId)
			.filter { it.availability != Availability.LOST }
		val visible = visibilityRepository.findById_CopyIdIn(allCopies.map { it.id!! })
			.filter { it.id.groupId in myGroups }
			.map { it.id.copyId }
			.toSet()
		val copies = allCopies.filter { it.id in visible }
		if (copies.isEmpty()) return emptyMap()

		// Every loan that isn't over yet – also one waiting for the handover.
		val lentOut = borrowingRepository.findByCopy_IdInAndStatusNot(copies.map { it.id!! }, BorrowingStatus.RETURNED)
			.map { it.copy.id!! }
			.toSet()

		return copies.groupBy { it.book.id!! }.mapNotNull { (bookId, bookCopies) ->
			when {
				bookCopies.any { it.availability == Availability.AVAILABLE && it.id !in lentOut } -> bookId to BookAvailability.AVAILABLE
				bookCopies.any { it.id in lentOut } -> bookId to BookAvailability.BORROWED
				else -> null
			}
		}.toMap()
	}
}
