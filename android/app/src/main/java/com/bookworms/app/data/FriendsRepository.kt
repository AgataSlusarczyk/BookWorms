package com.bookworms.app.data

import android.content.Context
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.FriendDto
import com.bookworms.app.data.network.FriendsApi
import com.bookworms.app.data.network.GroupSummaryDto
import com.bookworms.app.data.network.GroupsApi
import com.bookworms.app.data.network.InviteUserRequestDto
import com.bookworms.app.data.network.SendFriendRequestBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class Friend(
    val id: String,
    val name: String,
    val username: String,
    // Full address of their photo, or null for the gray circle.
    val photoUrl: String? = null,
)

data class FriendBook(
    val copyId: String,
    val isbn: String?,
    val title: String,
    val authors: String?,
    val publishedYear: Int?,
    val coverUrl: String?,
    val bookDescription: String?,
    val ownerNote: String?,
)

data class FriendRequest(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterUsername: String,
    val requesterPhotoUrl: String?,
)

data class GroupInvitation(
    val id: String,
    val groupId: String,
    val groupName: String,
    val inviterName: String,
)

data class FriendsOverview(
    val friends: List<Friend>,
    val incomingRequests: List<FriendRequest>,
    val groupInvitations: List<GroupInvitation> = emptyList(),
    val myGroups: List<GroupSummaryDto> = emptyList(),
    val isOffline: Boolean = false,
)

class FriendsRepository(
    private val context: Context,
    private val api: FriendsApi = ApiClient.friends,
    private val groupsApi: GroupsApi = ApiClient.groups,
) {

    private val prefs by lazy {
        context.getSharedPreferences("friends_cache", Context.MODE_PRIVATE)
    }

    suspend fun getOverview(): FriendsOverview {
        val friends = api.getFriends().map { it.toFriend() }
        saveCachedFriends(friends)

        val requests = api.getIncomingRequests().map {
            FriendRequest(it.id, it.requesterId, it.requesterName, it.requesterUsername, serverUrl(it.requesterPhotoUrl))
        }
        val groupInvitations = runCatching {
            groupsApi.myInvitations().map {
                GroupInvitation(it.id, it.groupId, it.groupName, it.inviterName)
            }
        }.getOrDefault(emptyList())

        val myGroups = runCatching { groupsApi.myGroups() }.getOrDefault(emptyList())

        return FriendsOverview(
            friends = friends,
            incomingRequests = requests,
            groupInvitations = groupInvitations,
            myGroups = myGroups,
            isOffline = false,
        )
    }

    suspend fun getFriendBooks(friendId: String): List<FriendBook> =
        api.getFriendBooks(friendId).map {
            FriendBook(
                copyId = it.copyId,
                isbn = it.isbn,
                title = it.title,
                authors = it.authors,
                publishedYear = it.publishedYear,
                coverUrl = it.coverUrl,
                bookDescription = it.bookDescription,
                ownerNote = it.ownerNote,
            )
        }

    fun getCachedOverview(): FriendsOverview? {
        val raw = prefs.getString(KEY_FRIENDS, null) ?: return null
        val friends = runCatching { ApiClient.json.decodeFromString<List<Friend>>(raw) }.getOrNull()
            ?: return null
        return FriendsOverview(
            friends = friends,
            incomingRequests = emptyList(),
            groupInvitations = emptyList(),
            myGroups = emptyList(),
            isOffline = true,
        )
    }

    private fun saveCachedFriends(friends: List<Friend>) {
        val raw = ApiClient.json.encodeToString(friends)
        prefs.edit().putString(KEY_FRIENDS, raw).apply()
    }

    suspend fun searchUsers(query: String): List<Friend> =
        api.searchUsers(query.trim()).map { it.toFriend() }

    suspend fun sendRequest(userId: String) {
        api.sendRequest(SendFriendRequestBody(userId))
    }

    suspend fun acceptRequest(requestId: String) {
        api.acceptRequest(requestId)
    }

    suspend fun rejectRequest(requestId: String) {
        api.rejectRequest(requestId)
    }

    suspend fun removeFriend(userId: String) {
        api.removeFriend(userId)
    }

    suspend fun inviteToGroup(groupId: String, userId: String) {
        val response = groupsApi.inviteUser(groupId, InviteUserRequestDto(userId))
        if (!response.isSuccessful) {
            throw IllegalStateException("Could not send group invitation (${response.code()})")
        }
    }

    suspend fun acceptGroupInvitation(invitationId: String) {
        groupsApi.acceptInvitation(invitationId)
    }

    suspend fun rejectGroupInvitation(invitationId: String) {
        groupsApi.rejectInvitation(invitationId)
    }

    private companion object {
        const val KEY_FRIENDS = "cached_friends_list"
    }
}

private fun FriendDto.toFriend() = Friend(id, name, username, serverUrl(photoUrl))
