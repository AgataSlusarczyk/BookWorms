package com.bookworms.app.data

import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.BookCopiesDto
import com.bookworms.app.data.network.LoanDto
import com.bookworms.app.data.network.LoansApi
import com.bookworms.app.data.network.RequestDto
import com.bookworms.app.data.network.SendRequestDto
import com.bookworms.app.ui.book.BookCopy
import com.bookworms.app.ui.book.CopyStatus
import com.bookworms.app.ui.book.Queue
import com.bookworms.app.ui.loans.BookRequest
import com.bookworms.app.ui.loans.Loan
import com.bookworms.app.ui.loans.Loans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.time.Instant
import java.util.UUID

// Loans and borrow requests for the Loans tabs.
// Every function throws when the server can't be reached or answers with an error.
class LoansRepository(private val api: LoansApi = ApiClient.loans) {

    // All four lists, asked for at the same time.
    // Kept on the phone too, for when the server can't be reached (savedLoans).
    internal suspend fun loans(): Loans = coroutineScope {
        val borrowed = async { api.borrowings() }
        val sent = async { api.sentRequests() }
        val lent = async { api.lent() }
        val received = async { api.receivedRequests() }
        val saved = SavedLoans(borrowed.await(), sent.await(), lent.await(), received.await())
        Session.user.value?.let { SavedData.saveLoans(it.id, saved) }
        saved.toLoans()
    }

    // The lists as they were the last time the server sent them, or null when there's no copy on the phone.
    internal suspend fun savedLoans(): Loans? {
        val user = Session.user.value ?: return null
        return SavedData.loans(user.id)?.toLoans()
    }

    private fun SavedLoans.toLoans() = Loans(
        borrowed = borrowed.map { it.toLoan(person = it.owner.name) },
        sentRequests = sent.map { it.toBookRequest(person = it.owner.name) },
        lent = lent.map { it.toLoan(person = it.borrower.name) },
        receivedRequests = received.map { it.toBookRequest(person = it.requester.name) },
    )

    // Requests to the user still waiting for an answer – for the Accept and Reject buttons on Notifications.
    internal suspend fun receivedRequests(): List<BookRequest> =
        api.receivedRequests().map { it.toBookRequest(person = it.requester.name) }

    suspend fun markReturned(loanId: String) {
        api.markReturned(loanId)
    }

    // Throws CannotAnswerException when the book can't be lent now or the request was already answered.
    suspend fun accept(requestId: String) = answering { api.accept(requestId) }

    // Throws CannotAnswerException when the request was already answered (or cancelled).
    suspend fun reject(requestId: String) = answering { api.reject(requestId) }

    // Throws CannotAnswerException when the owner already answered – then there's nothing to take back.
    suspend fun cancelRequest(requestId: String) = answering { api.cancelRequest(requestId) }

    // "Who has it" on Book details.
    suspend fun bookCopies(bookId: String): BookCopies = api.bookCopies(bookId).toBookCopies()

    // Throws CopyChangedException when the copy can't be borrowed anymore or the user already asked.
    // A random key, so a tap repeated on slow internet still makes one request.
    suspend fun sendRequest(copyId: String) =
        copyAction { api.sendRequest(copyId, SendRequestDto(idempotencyKey = UUID.randomUUID().toString())) }

    // Throws CopyChangedException when the copy can be borrowed right away – no need to wait.
    suspend fun joinQueue(copyId: String) = copyAction { api.joinQueue(copyId) }

    suspend fun leaveQueue(copyId: String) {
        api.leaveQueue(copyId)
    }

    private suspend fun answering(call: suspend () -> RequestDto) {
        try {
            call()
        } catch (e: HttpException) {
            throw if (e.code() == 409 || e.code() == 404) CannotAnswerException() else e
        }
    }

    private suspend fun copyAction(call: suspend () -> Unit) {
        try {
            call()
        } catch (e: HttpException) {
            throw if (e.code() == 409 || e.code() == 404) CopyChangedException() else e
        }
    }
}

class CannotAnswerException : Exception("The request can't be answered now")

// Someone else changed the copy in the meantime (lent it, took it back…), so the user's tap no longer fits.
class CopyChangedException : Exception("The copy changed in the meantime")

// "Who has it" on Book details: the user's group names and other people's copies in them.
data class BookCopies(val groups: List<String>, val copies: List<BookCopy>)

private fun BookCopiesDto.toBookCopies(): BookCopies {
    val groupNames = groups.associate { it.id to it.name }
    return BookCopies(
        groups = groups.map { it.name },
        copies = copies.map { copy ->
            val queue = Queue(position = copy.myQueuePosition, length = copy.queueLength)
            BookCopy(
                id = copy.copyId,
                ownerName = copy.owner.name,
                ownerPhotoUrl = serverUrl(copy.owner.photoUrl),
                status = when {
                    copy.borrowedUntil != null ->
                        CopyStatus.Borrowed(returnBy = localDate(copy.borrowedUntil), byMe = copy.borrowedByMe, queue = queue)
                    copy.availability == "AVAILABLE" -> CopyStatus.Available(addedOn = localDate(copy.addedAt))
                    else -> CopyStatus.NotLending(queue)
                },
                groups = copy.groupIds.mapNotNull { groupNames[it] },
                myRequestId = copy.myRequestId,
            )
        },
    )
}

private fun LoanDto.toLoan(person: String) = Loan(
    id = id,
    title = book.title,
    isbn = book.isbn,
    coverUrl = serverUrl(book.coverUrl),
    person = person,
    since = localDate(since),
    returnBy = localDate(dueDate),
)

private fun RequestDto.toBookRequest(person: String) = BookRequest(
    id = id,
    title = book.title,
    isbn = book.isbn,
    coverUrl = serverUrl(book.coverUrl),
    person = person,
    createdAt = Instant.parse(createdAt),
    canAccept = canAccept,
)
