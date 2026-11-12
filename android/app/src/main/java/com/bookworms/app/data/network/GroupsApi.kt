package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

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
}

@Serializable
data class GroupSummaryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val memberCount: Int,
    // ADMIN or MEMBER.
    val myRole: String,
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
