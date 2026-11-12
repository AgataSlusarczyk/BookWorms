package com.bookworms.server.groups

import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.copies.CopyGroupVisibilityRepository
import com.bookworms.server.groups.dto.GroupBookInfoResponse
import com.bookworms.server.groups.dto.GroupBookResponse
import com.bookworms.server.groups.dto.GroupDetailsResponse
import com.bookworms.server.groups.dto.GroupRequest
import com.bookworms.server.groups.dto.GroupSummaryResponse
import com.bookworms.server.groups.dto.InvitePreviewResponse
import com.bookworms.server.groups.dto.MemberResponse
import com.bookworms.server.loans.BorrowingRepository
import com.bookworms.server.loans.BorrowingStatus
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

// Groups: make one, join with a code, see who's in and what books it has, manage it.
// Someone outside a group gets "not found" for it, so nobody can poke at other people's groups.
@Service
class GroupService(
	private val groupRepository: GroupRepository,
	private val memberRepository: GroupMemberRepository,
	private val userRepository: UserRepository,
	private val copyRepository: BookCopyRepository,
	private val visibilityRepository: CopyGroupVisibilityRepository,
	private val borrowingRepository: BorrowingRepository,
) {

	private val random = SecureRandom()

	@Transactional(readOnly = true)
	fun myGroups(userId: UUID): List<GroupSummaryResponse> {
		val memberships = memberRepository.findById_UserIdOrderByJoinedAtAsc(userId)
		if (memberships.isEmpty()) return emptyList()
		val groupIds = memberships.map { it.id.groupId }
		val groups = groupRepository.findAllById(groupIds).associateBy { it.id!! }
		val counts = memberRepository.countMembers(groupIds).associate { (id, count) -> id as UUID to count as Long }
		return memberships.mapNotNull { membership ->
			val group = groups[membership.id.groupId] ?: return@mapNotNull null
			GroupSummaryResponse(
				id = group.id!!,
				name = group.name,
				description = group.description,
				memberCount = counts[group.id] ?: 0,
				myRole = membership.groupRole,
			)
		}
	}

	// The person who makes the group is its first admin, and their books show up in it.
	@Transactional
	fun create(userId: UUID, request: GroupRequest): GroupDetailsResponse {
		val group = groupRepository.save(
			Group(
				name = request.name.trim(),
				description = request.description?.trim()?.ifEmpty { null },
				inviteCode = newInviteCode(),
				createdBy = userId,
			),
		)
		addMember(userId, group.id!!, GroupRole.ADMIN)
		return details(userId, group.id!!)
	}

	@Transactional(readOnly = true)
	fun details(userId: UUID, groupId: UUID): GroupDetailsResponse {
		val membership = membership(userId, groupId)
		val group = group(groupId)
		val members = memberRepository.findById_GroupIdOrderByJoinedAtAsc(groupId)
		val names = names(members.map { it.id.userId })
		val canInvite = membership.groupRole == GroupRole.ADMIN || group.membersCanInvite
		return GroupDetailsResponse(
			id = group.id!!,
			name = group.name,
			description = group.description,
			myRole = membership.groupRole,
			membersCanInvite = group.membersCanInvite,
			inviteCode = if (canInvite) group.inviteCode else null,
			members = members.map {
				MemberResponse(
					id = it.id.userId,
					name = names[it.id.userId].orEmpty(),
					role = it.groupRole,
					joinedAt = it.joinedAt,
				)
			},
			createdAt = group.createdAt,
		)
	}

	// The group's library: every copy its members show in it, newest first, with who has it now.
	@Transactional(readOnly = true)
	fun books(userId: UUID, groupId: UUID): List<GroupBookResponse> {
		membership(userId, groupId)
		val copyIds = visibilityRepository.findById_GroupId(groupId).map { it.id.copyId }
		if (copyIds.isEmpty()) return emptyList()
		val copies = copyRepository.findByIdInAndDeletedAtIsNullOrderByCreatedAtDesc(copyIds)
		val loans = borrowingRepository.findByCopy_IdInAndStatusNot(copies.map { it.id!! }, BorrowingStatus.RETURNED)
			.associateBy { it.copy.id!! }
		val names = names(copies.map { it.ownerId } + loans.values.map { it.borrowerId })
		return copies.map { copy ->
			val loan = loans[copy.id]
			GroupBookResponse(
				copyId = copy.id!!,
				book = GroupBookInfoResponse(
					id = copy.book.id!!,
					isbn = copy.book.isbn,
					title = copy.book.title,
					authors = copy.book.authors,
					coverUrl = copy.book.coverUrl,
				),
				owner = PersonResponse(copy.ownerId, names[copy.ownerId].orEmpty()),
				availability = copy.availability,
				borrowedBy = loan?.let { PersonResponse(it.borrowerId, names[it.borrowerId].orEmpty()) },
			)
		}
	}

	// "Find group": shows the group behind a code before joining.
	@Transactional(readOnly = true)
	fun preview(userId: UUID, code: String): InvitePreviewResponse {
		val group = groupByCode(code)
		return InvitePreviewResponse(
			id = group.id!!,
			name = group.name,
			description = group.description,
			memberCount = memberRepository.countById_GroupId(group.id!!),
			alreadyMember = memberRepository.existsById(GroupMemberId(userId, group.id!!)),
		)
	}

	// Joining twice does nothing new – the app just opens the group.
	@Transactional
	fun join(userId: UUID, code: String): GroupDetailsResponse {
		val group = groupByCode(code)
		if (!memberRepository.existsById(GroupMemberId(userId, group.id!!))) {
			addMember(userId, group.id!!, GroupRole.MEMBER)
		}
		return details(userId, group.id!!)
	}

	@Transactional
	fun update(userId: UUID, groupId: UUID, request: GroupRequest): GroupDetailsResponse {
		requireAdmin(userId, groupId)
		val group = group(groupId)
		group.name = request.name.trim()
		group.description = request.description?.trim()?.ifEmpty { null }
		request.membersCanInvite?.let { group.membersCanInvite = it }
		return details(userId, groupId)
	}

	// The members, their books in it and its discussions go with it (the database removes them).
	@Transactional
	fun delete(userId: UUID, groupId: UUID) {
		requireAdmin(userId, groupId)
		groupRepository.deleteById(groupId)
	}

	// The old code stops working, so people who got it but didn't join yet can't anymore.
	@Transactional
	fun newInviteCode(userId: UUID, groupId: UUID): GroupDetailsResponse {
		val membership = membership(userId, groupId)
		val group = group(groupId)
		if (membership.groupRole != GroupRole.ADMIN && !group.membersCanInvite) {
			throw ApiException(HttpStatus.FORBIDDEN, "NOT_GROUP_ADMIN", "Only the group's admin can do this")
		}
		group.inviteCode = newInviteCode()
		return details(userId, groupId)
	}

	@Transactional
	fun makeAdmin(userId: UUID, groupId: UUID, memberId: UUID): GroupDetailsResponse {
		requireAdmin(userId, groupId)
		otherMember(groupId, memberId).groupRole = GroupRole.ADMIN
		return details(userId, groupId)
	}

	// Admins can't remove each other – an admin who wants out leaves on their own.
	@Transactional
	fun removeMember(userId: UUID, groupId: UUID, memberId: UUID): GroupDetailsResponse {
		requireAdmin(userId, groupId)
		val member = otherMember(groupId, memberId)
		if (member.groupRole == GroupRole.ADMIN) {
			throw ApiException(HttpStatus.CONFLICT, "MEMBER_IS_ADMIN", "An admin can't be removed from the group")
		}
		removeFromGroup(member)
		return details(userId, groupId)
	}

	// The last one out deletes the group. The last admin out makes the longest member the new admin,
	// so a group never stays without someone to look after it.
	@Transactional
	fun leave(userId: UUID, groupId: UUID) {
		val membership = membership(userId, groupId)
		removeFromGroup(membership)
		val left = memberRepository.findById_GroupIdOrderByJoinedAtAsc(groupId)
		when {
			left.isEmpty() -> groupRepository.deleteById(groupId)
			left.none { it.groupRole == GroupRole.ADMIN } -> left.first().groupRole = GroupRole.ADMIN
		}
	}

	private fun addMember(userId: UUID, groupId: UUID, role: GroupRole) {
		// Flushed now, because the next query is plain SQL and needs the row to be in the database already.
		memberRepository.saveAndFlush(GroupMember(GroupMemberId(userId, groupId), role))
		memberRepository.showAllCopiesInGroup(userId, groupId)
	}

	private fun removeFromGroup(member: GroupMember) {
		memberRepository.hideAllCopiesFromGroup(member.id.userId, member.id.groupId)
		memberRepository.delete(member)
		memberRepository.flush()
	}

	private fun membership(userId: UUID, groupId: UUID): GroupMember =
		memberRepository.findById(GroupMemberId(userId, groupId)).orElseThrow {
			ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found")
		}

	private fun requireAdmin(userId: UUID, groupId: UUID) {
		if (membership(userId, groupId).groupRole != GroupRole.ADMIN) {
			throw ApiException(HttpStatus.FORBIDDEN, "NOT_GROUP_ADMIN", "Only the group's admin can do this")
		}
	}

	// Someone else in the group. Yourself: use leave instead.
	private fun otherMember(groupId: UUID, memberId: UUID): GroupMember =
		memberRepository.findById(GroupMemberId(memberId, groupId)).orElseThrow {
			ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "This person isn't in the group")
		}

	private fun group(groupId: UUID): Group =
		groupRepository.findById(groupId).orElseThrow {
			ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found")
		}

	private fun groupByCode(code: String): Group =
		normalizeCode(code)?.let { groupRepository.findByInviteCode(it) }
			?: throw ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "No group has this code")

	private fun names(userIds: Collection<UUID>): Map<UUID, String> =
		userRepository.findAllById(userIds.toSet()).associate { it.id!! to it.name }

	// 8 characters in two halves, e.g. "K7QM-3XTP". No 0/O and 1/I, so it's easy to read out loud.
	private fun newInviteCode(): String {
		while (true) {
			val chars = (1..CodeLength).map { CodeChars[random.nextInt(CodeChars.length)] }.joinToString("")
			val code = chars.take(CodeLength / 2) + "-" + chars.drop(CodeLength / 2)
			if (!groupRepository.existsByInviteCode(code)) return code
		}
	}

	private companion object {
		const val CodeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
		const val CodeLength = 8
	}
}

// People type codes in many ways: "k7qm3xtp", "K7QM 3XTP", "k7qm-3xtp". All of them mean "K7QM-3XTP".
// null = can't be a code at all.
internal fun normalizeCode(text: String): String? {
	val chars = text.filter { it.isLetterOrDigit() }.uppercase()
	if (chars.length != 8) return null
	return chars.take(4) + "-" + chars.drop(4)
}
