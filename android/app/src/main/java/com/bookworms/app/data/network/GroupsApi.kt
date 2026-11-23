package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Groups on our server. Every call needs the login token.
// Someone outside a group gets 404 (code GROUP_NOT_FOUND) for it; admin-only calls answer 403 (NOT_GROUP_ADMIN).
interface GroupsApi {

    @GET("me/groups")
    suspend fun myGroups(): List<GroupSummaryDto>

    // The user becomes the group's first admin.
    @POST("groups")
    suspend fun create(@Body request: GroupRequestDto): GroupDetailsDto

    // The group behind a code, before joining. 404 (INVITE_NOT_FOUND) for a wrong code.
    @POST("groups/invite")
    suspend fun preview(@Body request: InviteCodeDto): InvitePreviewDto

    // Joins right away. Joining a group the user is in already just gives it back.
    @POST("groups/join")
    suspend fun join(@Body request: InviteCodeDto): GroupDetailsDto

    @GET("groups/{id}")
    suspend fun details(@Path("id") groupId: String): GroupDetailsDto

    // Every copy the members show in the group, newest first.
    @GET("groups/{id}/books")
    suspend fun books(@Path("id") groupId: String): List<GroupBookDto>

    @PATCH("groups/{id}")
    suspend fun update(@Path("id") groupId: String, @Body request: GroupRequestDto): GroupDetailsDto

    // Admin only. The photo as a file in a form, in the part called "photo", like the profile photo.
    // 400 when it isn't a picture, 413 when it's too big. The old photo is removed.
    @Multipart
    @PUT("groups/{id}/photo")
    suspend fun changePhoto(@Path("id") groupId: String, @Part photo: MultipartBody.Part): GroupDetailsDto

    // Admin only.
    @DELETE("groups/{id}/photo")
    suspend fun removePhoto(@Path("id") groupId: String): GroupDetailsDto

    // Response<Unit>, because the server answers 204 with no body.
    @DELETE("groups/{id}")
    suspend fun delete(@Path("id") groupId: String): Response<Unit>

    // The old code stops working.
    @POST("groups/{id}/invite-code")
    suspend fun newInviteCode(@Path("id") groupId: String): GroupDetailsDto

    @POST("groups/{id}/members/{memberId}/admin")
    suspend fun makeAdmin(@Path("id") groupId: String, @Path("memberId") memberId: String): GroupDetailsDto

    // 409 (MEMBER_IS_ADMIN) for another admin.
    @DELETE("groups/{id}/members/{memberId}")
    suspend fun removeMember(@Path("id") groupId: String, @Path("memberId") memberId: String): GroupDetailsDto

    @DELETE("me/groups/{id}")
    suspend fun leave(@Path("id") groupId: String): Response<Unit>

    // The group's chat, the newest first. before = the id of the oldest message the app has, for the page before it.
    // New messages come live on groups/{id}/messages/stream (see ChatRepository).
    @GET("groups/{id}/messages")
    suspend fun messages(
        @Path("id") groupId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<GroupMessageDto>

    // 400 for an empty message or one over 2000 characters.
    @POST("groups/{id}/messages")
    suspend fun sendMessage(@Path("id") groupId: String, @Body request: SendMessageDto): GroupMessageDto

    // People to invite, by their username: at least 2 letters, up to 20 people. Members and people
    // invited already are left out. Only for someone who can invite (403 NOT_GROUP_ADMIN otherwise).
    @GET("groups/{id}/search-users")
    suspend fun searchInvitableUsers(@Path("id") groupId: String, @Query("query") query: String): List<FriendDto>

    // 409 when they're in the group already (ALREADY_MEMBER) or invited already (INVITATION_EXISTS).
    // Response<Unit>, because the server answers 201 with no body.
    @POST("groups/{id}/invitations")
    suspend fun inviteUser(@Path("id") groupId: String, @Body request: InviteUserRequestDto): Response<Unit>

    // Invitations waiting for the user's answer, newest first.
    @GET("me/group-invitations")
    suspend fun myInvitations(): List<GroupInvitationDto>

    // Joins the group and gives it back.
    @POST("me/group-invitations/{invitationId}/accept")
    suspend fun acceptInvitation(@Path("invitationId") invitationId: String): GroupDetailsDto

    @POST("me/group-invitations/{invitationId}/reject")
    suspend fun rejectInvitation(@Path("invitationId") invitationId: String): Response<Unit>
}

@Serializable
data class GroupSummaryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    // ADMIN or MEMBER.
    val myRole: String,
    // A path on the server, e.g. "/photos/<file>.jpg". null = no photo.
    val photoUrl: String? = null,
)

@Serializable
data class GroupDetailsDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val myRole: String,
    val membersCanInvite: Boolean = false,
    // null when the user can't invite.
    val inviteCode: String? = null,
    val members: List<MemberDto> = emptyList(),
    val photoUrl: String? = null,
)

@Serializable
data class MemberDto(
    val id: String,
    val name: String,
    val role: String,
    val joinedAt: String,
    val photoUrl: String? = null,
)

@Serializable
data class InvitePreviewDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    val alreadyMember: Boolean = false,
    val photoUrl: String? = null,
)

@Serializable
data class GroupBookDto(
    val copyId: String,
    val book: LoanBookDto,
    val owner: PersonDto,
    // AVAILABLE, UNAVAILABLE or LOST.
    val availability: String,
    // Who has it now. null = on the owner's shelf.
    val borrowedBy: PersonDto? = null,
)

// Create group and Edit group. membersCanInvite = null leaves it as it is.
@Serializable
data class GroupRequestDto(
    val name: String,
    val description: String? = null,
    val membersCanInvite: Boolean? = null,
)

@Serializable
data class InviteCodeDto(
    val code: String,
)

@Serializable
data class GroupMessageDto(
    val id: String,
    val groupId: String,
    val sender: PersonDto,
    val content: String,
    // ISO time, e.g. "2026-11-20T09:14:30.016734Z".
    val createdAt: String,
)

@Serializable
data class SendMessageDto(
    val content: String,
)

@Serializable
data class InviteUserRequestDto(
    val userId: String,
)

@Serializable
data class GroupInvitationDto(
    val id: String,
    val groupId: String,
    val groupName: String,
    val inviterName: String,
    val createdAt: String,
)
