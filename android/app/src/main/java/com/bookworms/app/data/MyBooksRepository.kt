package com.bookworms.app.data

import com.bookworms.app.data.network.AddCopyRequestDto
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.ChangeAvailabilityRequestDto
import com.bookworms.app.data.network.CopiesApi
import com.bookworms.app.data.network.CopyDto
import com.bookworms.app.data.network.EditBookRequestDto
import com.bookworms.app.data.network.LoanDto
import com.bookworms.app.data.network.LoansApi
import com.bookworms.app.ui.library.BookForm
import com.bookworms.app.ui.library.OwnBook
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// The user's own books for My books, My copy and Add book.
// Every function throws when the server can't be reached or answers with an error.
class MyBooksRepository(
    private val copies: CopiesApi = ApiClient.copies,
    private val loans: LoansApi = ApiClient.loans,
) {

    // The books, each with who has it now. Both lists are asked for at the same time.
    internal suspend fun myBooks(): List<OwnBook> = coroutineScope {
        val myCopies = async { copies.myCopies() }
        val lent = async { loans.lent() }
        val loanOfCopy = lent.await().associateBy { it.copyId }
        myCopies.await().map { it.toOwnBook(loanOfCopy[it.id]) }
    }

    // Throws InvalidIsbnException when the server doesn't accept the ISBN.
    internal suspend fun add(form: BookForm): OwnBook {
        val request = AddCopyRequestDto(
            isbn = form.isbn.ifEmpty { null },
            title = form.title.trim(),
            authors = form.author.trim(),
            publisher = form.publisher.trim().ifEmpty { null },
            publishedYear = form.year,
        )
        val copy = try {
            copies.addCopy(request)
        } catch (e: HttpException) {
            throw if (e.code() == 400) InvalidIsbnException() else e
        }
        // A new book is on the shelf.
        return copy.toOwnBook(loan = null)
    }

    // The loan isn't in the server's answer, so it's kept from the book as the app knows it.
    internal suspend fun setAvailable(book: OwnBook, available: Boolean): OwnBook {
        val copy = copies.changeAvailability(
            book.id,
            ChangeAvailabilityRequestDto(if (available) Available else Unavailable),
        )
        return copy.toOwnBook(loan = null).withLoanOf(book)
    }

    internal suspend fun editBook(book: OwnBook, form: BookForm): OwnBook {
        val copy = copies.editBook(
            book.id,
            EditBookRequestDto(
                title = form.title.trim(),
                authors = form.author.trim(),
                publisher = form.publisher.trim().ifEmpty { null },
                publishedYear = form.year,
            ),
        )
        return copy.toOwnBook(loan = null).withLoanOf(book)
    }

    // Throws CopyLentException while someone has the book.
    suspend fun remove(copyId: String) {
        val response = copies.removeCopy(copyId)
        if (response.isSuccessful) return
        throw if (response.code() == 409) CopyLentException() else HttpException(response)
    }

    private companion object {
        const val Available = "AVAILABLE"
        const val Unavailable = "UNAVAILABLE"
    }
}

class InvalidIsbnException : Exception("The server didn't accept the ISBN")

class CopyLentException : Exception("The book is lent out right now")

private fun CopyDto.toOwnBook(loan: LoanDto?) = OwnBook(
    id = id,
    title = book.title,
    author = book.authors.orEmpty(),
    year = book.publishedYear,
    isbn = book.isbn,
    publisher = book.publisher,
    coverUrl = serverUrl(book.coverUrl),
    // UNAVAILABLE and LOST both mean nobody can borrow it.
    availableToBorrow = availability == "AVAILABLE",
    canEditBook = canEditBook,
    lentTo = loan?.borrower?.name,
    lentSince = loan?.since?.let(::localDate),
    returnBy = loan?.dueDate?.let(::localDate),
)

private fun OwnBook.withLoanOf(book: OwnBook) =
    copy(lentTo = book.lentTo, lentSince = book.lentSince, returnBy = book.returnBy)

// The server sends moments in time, e.g. "2026-11-11T19:49:34Z". The app shows the day on the phone's clock.
internal fun localDate(isoInstant: String): LocalDate =
    Instant.parse(isoInstant).atZone(ZoneId.systemDefault()).toLocalDate()
