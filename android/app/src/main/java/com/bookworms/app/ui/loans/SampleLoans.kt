package com.bookworms.app.ui.loans

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

// Sample data for Book details and Notifications. Loans gets the real ones from the server (LoansViewModel).
// TODO: replace once the server sends the copies of a book (with their ids) and notifications.

// Someone asked to borrow a book. person = who asked, or whose book it is (sent requests).
internal data class SampleRequest(val id: Int, val title: String, val person: String, val hoursAgo: Int)

// The user waits for a copy someone else has borrowed now. owner = whose copy it is.
internal data class QueueSpot(val title: String, val owner: String)

// How long a book is lent for when the owner says yes.
internal const val LoanDays = 14

// "Borrow" on Book details. The copies there are sample ones, with no id to send to the server,
// so these requests stay on the phone and don't show up in Loans.
internal val sentRequests = mutableStateListOf(
    SampleRequest(11, "Bieguni", "Anna K.", hoursAgo = 5),
)

internal val queueSpots = mutableStateListOf<QueueSpot>()

// The request shown in Notifications.
internal val incomingRequests = listOf(
    SampleRequest(31, "Solaris", "Krzysiek P.", hoursAgo = 2),
)

// The user's answers so far: request id → true = accepted, false = rejected.
internal val requestAnswers = mutableStateMapOf<Int, Boolean>()

internal fun hasSentRequest(title: String, owner: String) = sentRequests.any { it.title == title && it.person == owner }

internal fun sendRequest(title: String, owner: String) {
    // A new id that no other request has yet.
    val id = (sentRequests.maxOfOrNull { it.id } ?: 100) + 1
    sentRequests.add(0, SampleRequest(id, title, owner, hoursAgo = 0))
}

internal fun cancelRequest(title: String, owner: String) {
    sentRequests.removeAll { it.title == title && it.person == owner }
}
