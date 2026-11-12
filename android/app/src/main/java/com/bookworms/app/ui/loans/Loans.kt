package com.bookworms.app.ui.loans

import java.time.Instant
import java.time.LocalDate

// A book that changed hands. person = who it's from (Borrowing) or who has it (Lent).
internal data class Loan(
    val id: String,
    val title: String,
    // null = a book typed in by hand, which has no page to open.
    val isbn: String?,
    val coverUrl: String?,
    val person: String,
    val since: LocalDate,
    val returnBy: LocalDate,
)

// Someone asked to borrow a book. person = who asked (Requests) or whose book it is (Waiting for an answer).
internal data class BookRequest(
    val id: String,
    val title: String,
    val isbn: String?,
    val coverUrl: String?,
    val person: String,
    val createdAt: Instant,
    // Only for requests to the user: false while the book is lent out or not available.
    val canAccept: Boolean,
)

// Everything the Loans tabs show.
internal data class Loans(
    val borrowed: List<Loan>,
    val sentRequests: List<BookRequest>,
    val lent: List<Loan>,
    val receivedRequests: List<BookRequest>,
)
