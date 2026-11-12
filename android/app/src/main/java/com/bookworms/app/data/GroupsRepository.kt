package com.bookworms.app.data

import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.GroupBookDto
import com.bookworms.app.data.network.GroupDetailsDto
import com.bookworms.app.data.network.GroupRequestDto
import com.bookworms.app.data.network.GroupsApi
import com.bookworms.app.data.network.InviteCodeDto
import com.bookworms.app.ui.groups.GroupBook
import com.bookworms.app.ui.groups.GroupDetails
import com.bookworms.app.ui.groups.GroupMember
import com.bookworms.app.ui.groups.GroupSummary
import com.bookworms.app.ui.groups.InvitePreview
import retrofit2.HttpException
import retrofit2.Response

// Groups for the Groups tab, Join group, Group details, Edit group, Invite and My copy.
// Every function throws when the server can't be reached or answers with an error.
class GroupsRepository(private val api: GroupsApi = ApiClient.groups) {

    internal suspend fun myGroups(): List<GroupSummary> = api.myGroups().map {
        GroupSummary(id = it.id, name = it.name, memberCount = it.memberCount, isAdmin = it.myRole == Admin)
    }

    internal suspend fun create(name: String, description: String): GroupDetails =
        api.create(GroupRequestDto(name = name.trim(), description = description.trim().ifEmpty { null })).toDetails()

    // Throws InviteNotFoundException for a code no group has.
    internal suspend fun preview(code: String): InvitePreview = withCode {
        val group = api.preview(InviteCodeDto(code.trim()))
        InvitePreview(
            name = group.name,
            description = group.description.orEmpty(),
            memberCount = group.memberCount,
            alreadyMember = group.alreadyMember,
        )
    }

    // Throws InviteNotFoundException when the code stopped working in the meantime.
    internal suspend fun join(code: String): GroupDetails = withCode { api.join(InviteCodeDto(code.trim())).toDetails() }

    internal suspend fun details(groupId: String): GroupDetails = api.details(groupId).toDetails()

    internal suspend fun books(groupId: String): List<GroupBook> = api.books(groupId).map { it.toGroupBook() }

    internal suspend fun update(groupId: String, name: String, description: String, membersCanInvite: Boolean): GroupDetails =
        api.update(
            groupId,
            GroupRequestDto(name = name.trim(), description = description.trim(), membersCanInvite = membersCanInvite),
        ).toDetails()

    suspend fun delete(groupId: String) = api.delete(groupId).orThrow()

    internal suspend fun newInviteCode(groupId: String): GroupDetails = api.newInviteCode(groupId).toDetails()

    internal suspend fun makeAdmin(groupId: String, memberId: String): GroupDetails = api.makeAdmin(groupId, memberId).toDetails()

    internal suspend fun removeMember(groupId: String, memberId: String): GroupDetails =
        api.removeMember(groupId, memberId).toDetails()

    suspend fun leave(groupId: String) = api.leave(groupId).orThrow()

    private suspend fun <T> withCode(call: suspend () -> T): T =
        try {
            call()
        } catch (e: HttpException) {
            throw if (e.code() == 404) InviteNotFoundException() else e
        }

    private fun Response<Unit>.orThrow() {
        if (!isSuccessful) throw HttpException(this)
    }

    private companion object {
        const val Admin = "ADMIN"
    }
}

class InviteNotFoundException : Exception("No group has this code")

private fun GroupDetailsDto.toDetails() = GroupDetails(
    id = id,
    name = name,
    description = description.orEmpty(),
    isAdmin = myRole == "ADMIN",
    membersCanInvite = membersCanInvite,
    inviteCode = inviteCode,
    members = members.map {
        GroupMember(id = it.id, name = it.name, isAdmin = it.role == "ADMIN", joined = localDate(it.joinedAt))
    },
)

private fun GroupBookDto.toGroupBook() = GroupBook(
    copyId = copyId,
    isbn = book.isbn,
    title = book.title,
    author = book.authors.orEmpty(),
    coverUrl = serverUrl(book.coverUrl),
    owner = owner.name,
    borrowedBy = borrowedBy?.name,
    available = availability == "AVAILABLE",
)
