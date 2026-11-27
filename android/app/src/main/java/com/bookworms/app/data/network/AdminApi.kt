package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// What only a system admin can do. Everyone else gets 403 (code NOT_SYSTEM_ADMIN).
interface AdminApi {

    @GET("admin/stats")
    suspend fun stats(): AdminStatsDto

    // No query = the newest accounts. Otherwise those whose name, username or email has it.
    @GET("admin/users")
    suspend fun users(@Query("query") query: String? = null): List<AdminUserDto>

    // 409 with code CANNOT_BLOCK_SELF or CANNOT_BLOCK_ADMIN.
    @POST("admin/users/{id}/block")
    suspend fun block(@Path("id") userId: String): AdminUserDto

    @POST("admin/users/{id}/unblock")
    suspend fun unblock(@Path("id") userId: String): AdminUserDto

    // Every group, also the ones the admin isn't in. The newest first.
    @GET("admin/groups")
    suspend fun groups(): List<AdminGroupDto>

    // Response<Unit>, because the server answers 204 with no body – like the three below.
    @DELETE("admin/groups/{id}")
    suspend fun deleteGroup(@Path("id") groupId: String): Response<Unit>

    // Any book in the catalog, for everyone. The ISBN can't be changed.
    @PUT("admin/books/{id}")
    suspend fun editBook(@Path("id") bookId: String, @Body request: AdminEditBookRequestDto): BookDetailsDto

    // The book with every copy, loan, rating and place on a list.
    @DELETE("admin/books/{id}")
    suspend fun deleteBook(@Path("id") bookId: String): Response<Unit>

    // Nobody sees the review anymore, and it doesn't count in the average.
    @POST("admin/reviews/{id}/hide")
    suspend fun hideReview(@Path("id") reviewId: String): Response<Unit>

    // The newest first.
    @GET("admin/logs")
    suspend fun logs(): List<AdminLogDto>
}

@Serializable
data class AdminStatsDto(
    val users: Int = 0,
    val blockedUsers: Int = 0,
    val books: Int = 0,
    val copies: Int = 0,
    val groups: Int = 0,
    val reviews: Int = 0,
    val activeLoans: Int = 0,
)

@Serializable
data class AdminUserDto(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    val photoUrl: String? = null,
    // USER or SYSTEM_ADMIN.
    val role: String = "USER",
    // ACTIVE or BLOCKED.
    val status: String = "ACTIVE",
    // e.g. "2026-11-14T20:15:00Z".
    val createdAt: String,
)

@Serializable
data class AdminGroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val photoUrl: String? = null,
    val memberCount: Int = 0,
    val createdBy: PersonDto,
    val createdAt: String,
)

@Serializable
data class AdminLogDto(
    val id: String,
    val admin: PersonDto,
    // e.g. DELETE_BOOK or BLOCK_USER.
    val action: String,
    val targetType: String,
    val targetId: String,
    // What it was about, e.g. the book's title or "@username".
    val description: String? = null,
    val createdAt: String,
)

// The same fields as editing your own typed-in book.
@Serializable
data class AdminEditBookRequestDto(
    val title: String,
    val authors: String,
    val publisher: String? = null,
    val publishedYear: Int? = null,
    val description: String? = null,
)
