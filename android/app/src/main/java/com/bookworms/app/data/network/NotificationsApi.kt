package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// The user's notifications on our server. Every call needs the login token.
interface NotificationsApi {

    // The newest 100, newest first. The server makes them itself, when something happens to a request,
    // a loan or a queue the user is in.
    @GET("me/notifications")
    suspend fun notifications(): List<NotificationDto>

    // "Mark as read": all of them at once.
    // Response<Unit>, because the server answers 204 with no body.
    @POST("me/notifications/read")
    suspend fun markAllRead(): Response<Unit>

    // A tap on one notification. 204 with no body, like above.
    @POST("me/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String): Response<Unit>
}

@Serializable
data class NotificationDto(
    val id: String,
    // E.g. BORROW_REQUESTED – see NotificationType in the app.
    val type: String,
    // The request, loan or copy it's about – which one depends on the type.
    val referenceId: String,
    val read: Boolean,
    val createdAt: String,
    val bookTitle: String,
    val bookIsbn: String? = null,
    // The other person: who asked, who answered, or whose book it is.
    val person: PersonDto,
)
