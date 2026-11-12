package com.bookworms.server.groups

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface GroupRepository : JpaRepository<Group, UUID> {
	fun findByInviteCode(inviteCode: String): Group?
	fun existsByInviteCode(inviteCode: String): Boolean
}

interface GroupMemberRepository : JpaRepository<GroupMember, GroupMemberId> {

	// "Id_UserId" means id.userId – the person part of the key.

	// My groups, the ones I joined first at the top.
	fun findById_UserIdOrderByJoinedAtAsc(userId: UUID): List<GroupMember>

	// A group's members, the earliest first.
	fun findById_GroupIdOrderByJoinedAtAsc(groupId: UUID): List<GroupMember>

	fun countById_GroupId(groupId: UUID): Long

	fun countById_GroupIdAndGroupRole(groupId: UUID, groupRole: GroupRole): Long

	// How many people each group has, for the Groups list – one query for all of them.
	@Query("select m.id.groupId, count(m) from GroupMember m where m.id.groupId in :groupIds group by m.id.groupId")
	fun countMembers(groupIds: Collection<UUID>): List<Array<Any>>

	// Someone joined: their books show up in the group straight away, like a new book shows up in all their groups.
	@Modifying
	@Query(
		value = """
		insert into copy_group_visibility (copy_id, group_id)
		select id, :groupId from book_copies where owner_id = :userId and deleted_at is null
		on conflict do nothing
		""",
		nativeQuery = true,
	)
	fun showAllCopiesInGroup(userId: UUID, groupId: UUID)

	// Someone left or was removed: the group no longer sees their books.
	@Modifying
	@Query(
		value = """
		delete from copy_group_visibility
		where group_id = :groupId and copy_id in (select id from book_copies where owner_id = :userId)
		""",
		nativeQuery = true,
	)
	fun hideAllCopiesFromGroup(userId: UUID, groupId: UUID)
}
