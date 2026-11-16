package com.bookworms.app.data

import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.AddCopyRequestDto
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.ChangeAvailabilityRequestDto
import com.bookworms.app.data.network.ChangeDescriptionRequestDto
import com.bookworms.app.data.network.ChangeGroupsRequestDto
import com.bookworms.app.data.network.CopiesApi
import com.bookworms.app.data.network.CopyDto
import com.bookworms.app.data.network.EditBookRequestDto
import com.bookworms.app.data.network.LoanDto
import com.bookworms.app.data.network.LoansApi
import com.bookworms.app.ui.book.BookDetails
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
    // Kept on the phone too, for when the server can't be reached (savedMyBooks).
    internal suspend fun myBooks(): List<OwnBook> = coroutineScope {
        val myCopies = async { copies.myCopies() }
        val lent = async { loans.lent() }
        val saved = SavedMyBooks(myCopies.await(), lent.await())
        Session.user.value?.let { SavedData.saveMyBooks(it.id, saved) }
        saved.toOwnBooks()
    }

    // The books as they were the last time the server sent them, or null when there's no copy on the phone.
    internal suspend fun savedMyBooks(): List<OwnBook>? {
        val user = Session.user.value ?: return null
        return SavedData.myBooks(user.id)?.toOwnBooks()
    }

    // Book details of one of the user's books, from the copy of My books on the phone –
    // title, author, year, description and cover, without ratings and who has it.
    // null = not the user's book, or there's no copy on the phone.
    suspend fun savedBookDetails(isbn: String): BookDetails? {
        val user = Session.user.value ?: return null
        val mine = SavedData.myBooks(user.id)?.copies?.filter { it.book.isbn == isbn }.orEmpty()
        val book = mine.firstOrNull()?.book ?: return null
        return BookDetails(
            id = book.id,
            isbn = isbn,
            title = book.title,
            author = book.authors.orEmpty(),
            year = book.publishedYear,
            pages = null,
            genre = null,
            description = book.description,
            myDescription = mine.firstNotNullOfOrNull { it.description },
            coverUrl = serverUrl(book.coverUrl),
            copies = emptyList(),
            ownCopies = mine.size,
            publicComments = emptyList(),
            groupDiscussions = emptyList(),
        )
    }

    // How many books the user has – shown in Profile.
    suspend fun count(): Int = copies.myCopies().size

    // Throws InvalidIsbnException when the server doesn't accept the ISBN,
    // and AlreadyOwnedException when the user already has this book (unless addAnother).
    internal suspend fun add(form: BookForm, addAnother: Boolean = false): OwnBook {
        val request = AddCopyRequestDto(
            isbn = form.isbn.ifEmpty { null },
            title = form.title.trim(),
            authors = form.author.trim(),
            publisher = form.publisher.trim().ifEmpty { null },
            publishedYear = form.year,
            description = form.description.trim().ifEmpty { null },
            addAnother = addAnother,
        )
        val copy = try {
            copies.addCopy(request)
        } catch (e: HttpException) {
            throw when (e.code()) {
                400 -> InvalidIsbnException()
                409 -> AlreadyOwnedException()
                else -> e
            }
        }
        // A new book is on the shelf – on the phone's copy too, newest first like on My books.
        updateSaved { it.copy(copies = listOf(copy) + it.copies) }
        return copy.toOwnBook(loan = null)
    }

    // The loan isn't in the server's answer, so it's kept from the book as the app knows it.
    internal suspend fun setAvailable(book: OwnBook, available: Boolean): OwnBook {
        val copy = copies.changeAvailability(
            book.id,
            ChangeAvailabilityRequestDto(if (available) Available else Unavailable),
        )
        replaceSaved(copy)
        return copy.toOwnBook(loan = null).withLoanOf(book)
    }

    // The groups that see the book from now on. Empty = only the user.
    internal suspend fun setGroups(book: OwnBook, groupIds: Set<String>): OwnBook {
        val copy = copies.changeGroups(book.id, ChangeGroupsRequestDto(groupIds.toList()))
        replaceSaved(copy)
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
                description = form.description.trim().ifEmpty { null },
            ),
        )
        replaceSaved(copy)
        return copy.toOwnBook(loan = null).withLoanOf(book)
    }

    // The user's own description. The catalog's text unchanged, or nothing, means "use the catalog's" again.
    internal suspend fun setMyDescription(book: OwnBook, text: String): OwnBook {
        val mine = text.trim().takeIf { it.isNotEmpty() && it != book.description?.trim() }
        val copy = copies.changeDescription(book.id, ChangeDescriptionRequestDto(mine))
        replaceSaved(copy)
        return copy.toOwnBook(loan = null).withLoanOf(book)
    }

    // Throws CopyLentException while someone has the book.
    suspend fun remove(copyId: String) {
        val response = copies.removeCopy(copyId)
        if (response.isSuccessful) {
            updateSaved { saved -> saved.copy(copies = saved.copies.filterNot { it.id == copyId }) }
            return
        }
        throw if (response.code() == 409) CopyLentException() else HttpException(response)
    }

    // After a change the server saved, the copy on the phone gets it too,
    // so it's still there when the server can't be reached later.
    private suspend fun replaceSaved(copy: CopyDto) =
        updateSaved { saved -> saved.copy(copies = saved.copies.map { if (it.id == copy.id) copy else it }) }

    private suspend fun updateSaved(change: (SavedMyBooks) -> SavedMyBooks) {
        val user = Session.user.value ?: return
        // No copy yet (My books and Library never loaded) – the next time they load, it's saved whole.
        val saved = SavedData.myBooks(user.id) ?: return
        SavedData.saveMyBooks(user.id, change(saved))
    }

    private companion object {
        const val Available = "AVAILABLE"
        const val Unavailable = "UNAVAILABLE"
    }
}

class InvalidIsbnException : Exception("The server didn't accept the ISBN")

class AlreadyOwnedException : Exception("The user already has this book")

class CopyLentException : Exception("The book is lent out right now")

private fun SavedMyBooks.toOwnBooks(): List<OwnBook> {
    val loanOfCopy = lent.associateBy { it.copyId }
    return copies.map { it.toOwnBook(loanOfCopy[it.id]) }
}

private fun CopyDto.toOwnBook(loan: LoanDto?) = OwnBook(
    id = id,
    title = book.title,
    author = book.authors.orEmpty(),
    year = book.publishedYear,
    isbn = book.isbn,
    publisher = book.publisher,
    description = book.description,
    myDescription = description,
    coverUrl = serverUrl(book.coverUrl),
    // UNAVAILABLE and LOST both mean nobody can borrow it.
    availableToBorrow = availability == "AVAILABLE",
    canEditBook = canEditBook,
    groupIds = groupIds.toSet(),
    lentTo = loan?.borrower?.name,
    lentSince = loan?.since?.let(::localDate),
    returnBy = loan?.dueDate?.let(::localDate),
)

private fun OwnBook.withLoanOf(book: OwnBook) =
    copy(lentTo = book.lentTo, lentSince = book.lentSince, returnBy = book.returnBy)

// The server sends moments in time, e.g. "2026-11-11T19:49:34Z". The app shows the day on the phone's clock.
internal fun localDate(isoInstant: String): LocalDate =
    Instant.parse(isoInstant).atZone(ZoneId.systemDefault()).toLocalDate()
