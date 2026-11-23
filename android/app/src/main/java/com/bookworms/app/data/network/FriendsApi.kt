package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// A friend, or someone found by search. The server never sends emails here.
@Serializable
data class FriendDto(
    val id: String,
    val name: String,
    val username: String = "",
    // A path on the server, e.g. "/photos/<file>.jpg". null = no photo.
    val photoUrl: String? = null,
)

@Serializable
data class FriendBookDto(
    val copyId: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val publishedYear: Int? = null,
    val coverUrl: String? = null,
    val bookDescription: String? = null,
    val ownerNote: String? = null,
)

@Serializable
data class FriendRequestDto(
    val id: String,
    val requesterId: String,
    val requesterName: String,
    val requesterUsername: String = "",
    val requesterPhotoUrl: String? = null,
)

// By id, from search.
@Serializable
data class SendFriendRequestBody(
    val userId: String,
)

interface FriendsApi {

    @GET("friends")
    suspend fun getFriends(): List<FriendDto>

    @GET("friends/{friendId}/books")
    suspend fun getFriendBooks(@Path("friendId") friendId: String): List<FriendBookDto>

    @GET("friends/requests")
    suspend fun getIncomingRequests(): List<FriendRequestDto>

    @GET("friends/search")
    suspend fun searchUsers(@Query("query") query: String): List<FriendDto>

    @POST("friends/requests")
    suspend fun sendRequest(@Body body: SendFriendRequestBody)

    @POST("friends/requests/{id}/accept")
    suspend fun acceptRequest(@Path("id") requestId: String)

    @POST("friends/requests/{id}/reject")
    suspend fun rejectRequest(@Path("id") requestId: String)

    @DELETE("friends/{userId}")
    suspend fun removeFriend(@Path("userId") userId: String)
}
