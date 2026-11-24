package com.bookworms.app.data

import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.NotificationDto
import com.bookworms.app.data.network.NotificationsApi
import com.bookworms.app.ui.notifications.NotificationItem
import com.bookworms.app.ui.notifications.NotificationType
import retrofit2.HttpException
import java.time.Instant

// The Notifications screen. Every function throws when the server can't be reached or answers with an error.
class NotificationsRepository(private val api: NotificationsApi = ApiClient.notifications) {

    // Newest first. Types this app doesn't know (from a newer server) are left out.
    internal suspend fun notifications(): List<NotificationItem> = api.notifications().mapNotNull { it.toItem() }

    suspend fun markAllRead() {
        val response = api.markAllRead()
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun markRead(id: String) {
        val response = api.markRead(id)
        if (!response.isSuccessful) throw HttpException(response)
    }
}

private fun NotificationDto.toItem(): NotificationItem? {
    val type = when (type) {
        "BORROW_REQUESTED" -> NotificationType.BorrowRequested
        "REQUEST_ACCEPTED" -> NotificationType.RequestAccepted
        "REQUEST_REJECTED" -> NotificationType.RequestRejected
        "REQUEST_CANCELED" -> NotificationType.RequestCanceled
        "RETURN_CONFIRMED" -> NotificationType.ReturnConfirmed
        "RETURN_DUE" -> NotificationType.ReturnDue
        "RETURN_OVERDUE" -> NotificationType.ReturnOverdue
        "COPY_AVAILABLE" -> NotificationType.CopyAvailable
        else -> return null
    }
    return NotificationItem(
        id = id,
        type = type,
        referenceId = referenceId,
        read = read,
        createdAt = Instant.parse(createdAt),
        bookTitle = bookTitle,
        bookIsbn = bookIsbn,
        personName = person.name,
        personPhotoUrl = serverUrl(person.photoUrl),
    )
}
