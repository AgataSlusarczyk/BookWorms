package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

// Loans on our server. Every call needs the login token.
interface LoansApi {

    // The user's books other people have now. Soonest return date first.
    @GET("me/lent")
    suspend fun lent(): List<LoanDto>
}

// One loan, for the Borrowing and Lent tabs.
@Serializable
data class LoanDto(
    val id: String,
    // PENDING_HANDOVER, BORROWED or PENDING_RETURN (returned loans aren't sent).
    val status: String,
    // When the owner said yes. Dates come as ISO-8601, e.g. "2026-11-11T19:49:34.123Z".
    val since: String,
    val dueDate: String,
    val copyId: String,
    val book: LoanBookDto,
    // Who has the book.
    val borrower: PersonDto,
    // Whose book it is.
    val owner: PersonDto,
)

@Serializable
data class LoanBookDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val authors: String? = null,
    val coverUrl: String? = null,
)

@Serializable
data class PersonDto(
    val id: String,
    val name: String,
)
