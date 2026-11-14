package com.bookworms.app.ui.loans

import androidx.compose.runtime.mutableStateMapOf

// Sample data for Notifications. Loans and Book details get the real ones from the server.
// TODO: replace once Notifications are connected to the server.

// Someone asked to borrow a book. person = who asked, or whose book it is (sent requests).
internal data class SampleRequest(val id: Int, val title: String, val person: String, val hoursAgo: Int)

// How long a book is lent for when the owner says yes.
internal const val LoanDays = 14

// The request shown in Notifications.
internal val incomingRequests = listOf(
    SampleRequest(31, "Solaris", "Krzysiek P.", hoursAgo = 2),
)

// The user's answers so far: request id → true = accepted, false = rejected.
internal val requestAnswers = mutableStateMapOf<Int, Boolean>()
