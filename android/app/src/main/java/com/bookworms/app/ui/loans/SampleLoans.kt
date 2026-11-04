package com.bookworms.app.ui.loans

import androidx.compose.runtime.mutableStateListOf

// Someone asked to borrow a book. person = who asked (Requests) or whose book it is (Borrowing).
internal data class LoanRequest(val id: Int, val title: String, val person: String, val hoursAgo: Int)

// The user waits for a copy someone else has borrowed now. owner = whose copy it is.
internal data class QueueSpot(val title: String, val owner: String)

// How long a book is lent for when the owner says yes.
internal const val LoanDays = 14

// TODO: the user's requests and queues from the server.
// Kept outside the screens, so "Borrow" on Book details shows up in Loans under "Waiting for an answer".
internal val sentRequests = mutableStateListOf(
    LoanRequest(11, "Bieguni", "Anna K.", hoursAgo = 5),
)

internal val queueSpots = mutableStateListOf<QueueSpot>()

internal fun hasSentRequest(title: String, owner: String) = sentRequests.any { it.title == title && it.person == owner }

internal fun sendRequest(title: String, owner: String) {
    // A new id that no other request has yet.
    val id = (sentRequests.maxOfOrNull { it.id } ?: 100) + 1
    sentRequests.add(0, LoanRequest(id, title, owner, hoursAgo = 0))
}

internal fun cancelRequest(title: String, owner: String) {
    sentRequests.removeAll { it.title == title && it.person == owner }
}
