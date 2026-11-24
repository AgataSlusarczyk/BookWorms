package com.bookworms.app.ui.notifications

import java.time.Instant

// One notification on the Notifications screen.
internal data class NotificationItem(
    val id: String,
    val type: NotificationType,
    // The request, loan or copy it's about – which one depends on the type (see NotificationType).
    val referenceId: String,
    val read: Boolean,
    val createdAt: Instant,
    val bookTitle: String,
    // For opening Book details; null for a book added by hand without one.
    val bookIsbn: String?,
    // The other person: who asked, who answered, or whose book it is.
    val personName: String,
    // Full address of their photo, or null for the gray circle.
    val personPhotoUrl: String?,
)

// What happened. The comment says who gets it and what referenceId points to.
internal enum class NotificationType {
    // To the owner: someone asks to borrow their book. Request.
    BorrowRequested,
    // To the one who asked: the owner said yes. Request.
    RequestAccepted,
    // To the one who asked: the owner said no, or removed the book. Request.
    RequestRejected,
    // To the owner: the one who asked took it back. Request.
    RequestCanceled,
    // To the one who borrowed: the owner got the book back. Loan.
    ReturnConfirmed,
    // To the one who borrowed: the return date is close. Loan. The server doesn't send it yet.
    ReturnDue,
    // To the one who borrowed: the return date has passed. Loan. The server doesn't send it yet.
    ReturnOverdue,
    // To everyone in the queue: the book can be borrowed again. Copy.
    CopyAvailable,
}
