package com.bookworms.server.loans

import com.bookworms.server.books.BookNotFoundException
import com.bookworms.server.books.BookRepository
import com.bookworms.server.copies.Availability
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.copies.CopyGroupVisibilityRepository
import com.bookworms.server.groups.GroupMemberRepository
import com.bookworms.server.groups.GroupRepository
import com.bookworms.server.loans.dto.BookCopiesResponse
import com.bookworms.server.loans.dto.GroupNameResponse
import com.bookworms.server.loans.dto.OthersCopyResponse
import com.bookworms.server.users.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import com.bookworms.server.users.people
import com.bookworms.server.users.person

// "Who has it" on Book details: who in my groups has this book, can I borrow it, and did I already ask.
// My own copies aren't here – the app counts them from my books.
@Service
class BookCopiesService(
	private val bookRepository: BookRepository,
	private val copyRepository: BookCopyRepository,
	private val visibilityRepository: CopyGroupVisibilityRepository,
	private val memberRepository: GroupMemberRepository,
	private val groupRepository: GroupRepository,
	private val borrowingRepository: BorrowingRepository,
	private val requestRepository: BorrowRequestRepository,
	private val queueRepository: QueueRepository,
	private val userRepository: UserRepository,
) {

	@Transactional(readOnly = true)
	fun copies(userId: UUID, bookId: UUID): BookCopiesResponse {
		if (!bookRepository.existsById(bookId)) throw BookNotFoundException()

		val myGroupIds = memberRepository.findById_UserIdOrderByJoinedAtAsc(userId).map { it.id.groupId }
		val groupNames = groupRepository.findAllById(myGroupIds).associate { it.id!! to it.name }
		val groups = myGroupIds.mapNotNull { id -> groupNames[id]?.let { GroupNameResponse(id, it) } }
		if (groups.isEmpty()) return BookCopiesResponse(groups = emptyList(), copies = emptyList())

		// A book has only a few copies, so they're all read and then kept to the ones shown in my groups.
		// A lost copy can't be borrowed or waited for, so it isn't shown.
		val allCopies = copyRepository.findByBook_IdAndOwnerIdNotAndDeletedAtIsNullOrderByCreatedAtAsc(bookId, userId)
			.filter { it.availability != Availability.LOST }
		val myGroupSet = myGroupIds.toSet()
		val groupsOfCopy = visibilityRepository.findById_CopyIdIn(allCopies.map { it.id!! })
			.filter { it.id.groupId in myGroupSet }
			.groupBy({ it.id.copyId }, { it.id.groupId })
		val copies = allCopies.filter { groupsOfCopy.containsKey(it.id) }
		if (copies.isEmpty()) return BookCopiesResponse(groups = groups, copies = emptyList())

		val copyIds = copies.map { it.id!! }
		val loans = borrowingRepository.findByCopy_IdInAndStatusNot(copyIds, BorrowingStatus.RETURNED)
			.associateBy { it.copy.id!! }
		val myRequests = requestRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, RequestStatus.PENDING)
			.associate { it.copy.id!! to it.id!! }
		val myQueues = queueRepository.findById_UserIdOrderByCreatedAtAsc(userId).associateBy { it.id.copyId }
		val people = userRepository.people(copies.map { it.ownerId }.toSet())

		return BookCopiesResponse(
			groups = groups,
			copies = copies.map { copy ->
				val loan = loans[copy.id]
				OthersCopyResponse(
					copyId = copy.id!!,
					owner = people.person(copy.ownerId),
					// In the order of my groups, so the app shows them the same way everywhere.
					groupIds = myGroupIds.filter { it in groupsOfCopy.getValue(copy.id!!) },
					availability = copy.availability,
					addedAt = copy.createdAt,
					borrowedUntil = loan?.dueDate,
					borrowedByMe = loan?.borrowerId == userId,
					myRequestId = myRequests[copy.id],
					myQueuePosition = myQueues[copy.id]?.let {
						queueRepository.countById_CopyIdAndCreatedAtLessThanEqual(copy.id!!, it.createdAt)
					},
					queueLength = queueRepository.countById_CopyId(copy.id!!),
				)
			},
		)
	}
}
