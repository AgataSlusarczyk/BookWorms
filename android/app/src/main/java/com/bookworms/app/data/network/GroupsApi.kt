package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Groups on our server. Every call needs the login token.
interface GroupsApi {

    @GET("me/groups")
    suspend fun myGroups(): List<GroupSummaryDto>

    @POST("groups")
    suspend fun create(@Body request: GroupRequestDto): GroupDetailsDto

    @POST("groups/invite")
    suspend fun preview(@Body request: InviteCodeDto): InvitePreviewDto

    @POST("groups/join")
    suspend fun join(@Body request: InviteCodeDto): GroupDetailsDto

    @GET("groups/{id}")
    suspend fun details(@Path("id") groupId: String): GroupDetailsDto

    @GET("groups/{id}/books")
    suspend fun books(@Path("id") groupId: String): List<GroupBookDto>

    @PATCH("groups/{id}")
    suspend fun update(@Path("id") groupId: String, @Body request: GroupRequestDto): GroupDetailsDto

    @DELETE("groups/{id}")
    suspend fun delete(@Path("id") groupId: String): Response<Unit>

    @POST("groups/{id}/invite-code")
    suspend fun newInviteCode(@Path("id") groupId: String): GroupDetailsDto

    @POST("groups/{id}/members/{memberId}/admin")
    suspend fun makeAdmin(@Path("id") groupId: String, @Path("memberId") memberId: String): GroupDetailsDto

    @DELETE("groups/{id}/members/{memberId}")
    suspend fun removeMember(@Path("id") groupId: String, @Path("memberId") memberId: String): GroupDetailsDto

    @DELETE("me/groups/{id}")
    suspend fun leave(@Path("id") groupId: String): Response<Unit>

    @GET("groups/{id}/messages")
    suspend fun messages(
        @Path("id") groupId: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null,
    ): List<GroupMessageDto>

    @POST("groups/{id}/messages")
    suspend fun sendMessage(@Path("id") groupId: String, @Body request: SendMessageDto): GroupMessageDto

    // Group invitations (Option B)
    @GET("groups/{id}/search-users")
    suspend fun searchInvitableUsers(
        @Path("id") groupId: String,
        @Query("query") query: String,
    ): List<FriendDto>

    @POST("groups/{id}/invitations")
    suspend fun inviteUser(
        @Path("id") groupId: String,
        @Body request: InviteUserRequestDto,
    ): Response<Unit>

    @GET("me/group-invitations")
    suspend fun myInvitations(): List<GroupInvitationDto>

    @POST("me/group-invitations/{invitationId}/accept")
    suspend fun acceptInvitation(@Path("invitationId") invitationId: String): GroupDetailsDto

    @POST("me/group-invitations/{invitationId}/reject")
    suspend fun rejectInvitation(@Path("invitationId") invitationId: String): Response<Unit>
}

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

@Serializable
data class GroupSummaryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    val myRole: String,
)

@Serializable
data class GroupDetailsDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val myRole: String,
    val membersCanInvite: Boolean = false,
    val inviteCode: String? = null,
    val members: List<MemberDto> = emptyList(),
)

@Serializable
data class MemberDto(
    val id: String,
    val name: String,
    val role: String,
    val joinedAt: String,
)

@Serializable
data class InvitePreviewDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    val alreadyMember: Boolean = false,
)

@Serializable
data class GroupBookDto(
    val copyId: String,
    val book: LoanBookDto,
    val owner: PersonDto,
    val availability: String,
    val borrowedBy: PersonDto? = null,
)

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
    val createdAt: String,
)

@Serializable
data class SendMessageDto(
    val content: String,
)