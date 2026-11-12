package com.bookworms.app.data

import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.LoanDto
import com.bookworms.app.data.network.LoansApi
import com.bookworms.app.data.network.RequestDto
import com.bookworms.app.ui.loans.BookRequest
import com.bookworms.app.ui.loans.Loan
import com.bookworms.app.ui.loans.Loans
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.time.Instant

// Loans and borrow requests for the Loans tabs.
// Every function throws when the server can't be reached or answers with an error.
class LoansRepository(private val api: LoansApi = ApiClient.loans) {

    // All four lists, asked for at the same time.
    internal suspend fun loans(): Loans = coroutineScope {
        val borrowed = async { api.borrowings() }
        val sent = async { api.sentRequests() }
        val lent = async { api.lent() }
        val received = async { api.receivedRequests() }
        Loans(
            borrowed = borrowed.await().map { it.toLoan(person = it.owner.name) },
            sentRequests = sent.await().map { it.toBookRequest(person = it.owner.name) },
            lent = lent.await().map { it.toLoan(person = it.borrower.name) },
            receivedRequests = received.await().map { it.toBookRequest(person = it.requester.name) },
        )
    }

    suspend fun markReturned(loanId: String) {
        api.markReturned(loanId)
    }

    // Throws CannotAnswerException when the book can't be lent now or the request was already answered.
    suspend fun accept(requestId: String) = answering { api.accept(requestId) }

    // Throws CannotAnswerException when the request was already answered (or cancelled).
    suspend fun reject(requestId: String) = answering { api.reject(requestId) }

    private suspend fun answering(call: suspend () -> RequestDto) {
        try {
            call()
        } catch (e: HttpException) {
            throw if (e.code() == 409 || e.code() == 404) CannotAnswerException() else e
        }
    }
}

class CannotAnswerException : Exception("The request can't be answered now")

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
