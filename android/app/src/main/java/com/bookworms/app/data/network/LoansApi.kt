package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Loans and borrow requests on our server. Every call needs the login token.
interface LoansApi {

    // Books the user has from others now. Soonest return date first.
    @GET("me/borrowings")
    suspend fun borrowings(): List<LoanDto>

    // The user's books other people have now. Soonest return date first.
    @GET("me/lent")
    suspend fun lent(): List<LoanDto>

    // The book is back. Only the owner can say so.
    @POST("borrowings/{id}/return")
    suspend fun markReturned(@Path("id") loanId: String): LoanDto

    // What the user asked for and is still waiting for. Newest first.
    @GET("me/requests/sent")
    suspend fun sentRequests(): List<RequestDto>

    // Others asking to borrow the user's books, still waiting for an answer. Newest first.
    @GET("me/requests/received")
    suspend fun receivedRequests(): List<RequestDto>

    // Answers 409 when the book can't be lent now (COPY_LENT, COPY_NOT_AVAILABLE)
    // or the request was already answered (REQUEST_NOT_PENDING).
    @POST("requests/{id}/accept")
    suspend fun accept(@Path("id") requestId: String): RequestDto

    @POST("requests/{id}/reject")
    suspend fun reject(@Path("id") requestId: String): RequestDto

    // "Who has it" on Book details: other people's copies of this book in the user's groups.
    @GET("books/{bookId}/copies")
    suspend fun bookCopies(@Path("bookId") bookId: String): BookCopiesDto

    // Asks the owner to lend the copy. Answers 409 when it can't be lent now or the user already asked.
    // The same key sent twice (a tap repeated on slow internet) gives back the first request, not a second one.
    @POST("copies/{copyId}/requests")
    suspend fun sendRequest(@Path("copyId") copyId: String, @Body request: SendRequestDto): RequestDto

    // The user takes their request back. Answers 409 when the owner already answered.
    @POST("requests/{id}/cancel")
    suspend fun cancelRequest(@Path("id") requestId: String): RequestDto

    // Waits for a copy that's lent out or not lent right now. Answers 409 when it can be borrowed right away.
    @POST("copies/{copyId}/queue")
    suspend fun joinQueue(@Path("copyId") copyId: String)

    @DELETE("copies/{copyId}/queue")
    suspend fun leaveQueue(@Path("copyId") copyId: String)
}

@Serializable
data class SendRequestDto(val idempotencyKey: String)

@Serializable
data class BookCopiesDto(
    // All the user's groups, the ones they joined first at the top.
    val groups: List<GroupNameDto>,
    val copies: List<OthersCopyDto>,
)

@Serializable
data class GroupNameDto(
    val id: String,
    val name: String,
)

@Serializable
data class OthersCopyDto(
    val copyId: String,
    val owner: PersonDto,
    // Ids of the user's groups that see this copy.
    val groupIds: List<String>,
    // AVAILABLE or UNAVAILABLE (the owner doesn't lend it now). A lent-out copy is AVAILABLE – see borrowedUntil.
    val availability: String,
    val addedAt: String,
    // When it's due back, or null when nobody has it now.
    val borrowedUntil: String? = null,
    val borrowedByMe: Boolean = false,
    // The user's request waiting for an answer, or null.
    val myRequestId: String? = null,
    // The user's place in the queue (1 = next), or null when they don't wait.
    val myQueuePosition: Int? = null,
    // How many people wait, the user included.
    val queueLength: Int = 0,
)

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

// One borrow request, for Loans and Notifications.
@Serializable
data class RequestDto(
    val id: String,
    // PENDING for everything the lists send.
    val status: String,
    val createdAt: String,
    val copyId: String,
    val book: LoanBookDto,
    // Who asked.
    val requester: PersonDto,
    // Whose book it is.
    val owner: PersonDto,
    // Only for the owner: false while the book is lent out or not available, so Accept is grayed out.
    val canAccept: Boolean = false,
)

// The book of a loan or a request.
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
