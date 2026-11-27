package com.bookworms.app.ui.book

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookworms.app.data.AdminRepository
import com.bookworms.app.data.BookRepository
import com.bookworms.app.data.CannotAnswerException
import com.bookworms.app.data.CopyChangedException
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.LoansRepository
import com.bookworms.app.data.MyBooksRepository
import com.bookworms.app.data.ShelfRepository
import com.bookworms.app.data.ShelfState
import com.bookworms.app.data.load
import com.bookworms.app.data.loadOrSaved
import com.bookworms.app.data.withCopies
import com.bookworms.app.data.withReviews
import com.bookworms.app.ui.library.BookForm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// One book from the server, for Book details and Add comment.
class BookDetailsViewModel(
    private val isbn: String,
    private val repository: BookRepository = BookRepository(),
    private val loans: LoansRepository = LoansRepository(),
    private val myBooks: MyBooksRepository = MyBooksRepository(),
    private val shelf: ShelfRepository = ShelfRepository(),
    private val admin: AdminRepository = AdminRepository(),
) : ViewModel() {

    private val _book = MutableStateFlow<LoadState<BookDetails>>(LoadState.Loading)
    val book: StateFlow<LoadState<BookDetails>> = _book.asStateFlow()

    // true when the last rating didn't reach the server. The screen says so once, then calls ratingErrorShown().
    private val _ratingFailed = MutableStateFlow(false)
    val ratingFailed: StateFlow<Boolean> = _ratingFailed.asStateFlow()

    private var rating: Job? = null

    // true when the last heart or reading status didn't reach the server. Said once, then shelfErrorShown().
    private val _shelfFailed = MutableStateFlow(false)
    val shelfFailed: StateFlow<Boolean> = _shelfFailed.asStateFlow()

    private var shelfChange: Job? = null

    // The copy whose button was just tapped, while the server answers. Its button is grayed out until then.
    private val _busyCopy = MutableStateFlow<String?>(null)
    val busyCopy: StateFlow<String?> = _busyCopy.asStateFlow()

    // What to tell the user after a tap on a copy's button. The screen says it once, then calls copyMessageShown().
    private val _copyMessage = MutableStateFlow<CopyMessage?>(null)
    val copyMessage: StateFlow<CopyMessage?> = _copyMessage.asStateFlow()

    // true while the server answers an admin's change (edit, delete, hide). The buttons wait until then.
    private val _adminBusy = MutableStateFlow(false)
    val adminBusy: StateFlow<Boolean> = _adminBusy.asStateFlow()

    // What happened to an admin's change. Each one is handled once (Book details, or Edit book on top of it).
    private val _adminEvents = Channel<AdminBookEvent>(Channel.BUFFERED)
    val adminEvents: Flow<AdminBookEvent> = _adminEvents.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        _book.value = LoadState.Loading
        viewModelScope.launch { reload() }
    }

    // Without the server, the user's own books still open – from the copy of My books on the phone.
    private suspend fun reload() {
        _book.value = loadOrSaved(saved = { myBooks.savedBookDetails(isbn) }) { repository.details(isbn) }
    }

    // Admin only: changes the book for everyone. The page loads again with the new details.
    internal fun editBook(form: BookForm) = adminChange(AdminBookEvent.Edited) { book ->
        admin.editBook(book.id, form)
        reload()
    }

    // Admin only: the book is gone for everyone, with its copies, loans and ratings. The screen closes.
    fun deleteBook() = adminChange(AdminBookEvent.Deleted) { book -> admin.deleteBook(book.id) }

    // Admin only: nobody sees the review anymore, and it no longer counts in the average.
    fun hideReview(reviewId: String) = adminChange(AdminBookEvent.ReviewHidden) { book ->
        admin.hideReview(reviewId)
        val reviews = repository.reviews(book.id)
        updateBook { it.withReviews(reviews) }
    }

    private fun adminChange(success: AdminBookEvent, call: suspend (BookDetails) -> Unit) {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        if (_adminBusy.value) return
        _adminBusy.value = true
        viewModelScope.launch {
            val event = try {
                call(book)
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "An admin change of the book failed", e)
                AdminBookEvent.Failed
            }
            _adminBusy.value = false
            _adminEvents.send(event)
        }
    }

    // stars = 1–5, or null to take the rating back.
    // The stars change on the screen right away; if the server doesn't take them, they go back.
    fun rate(stars: Int?) {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        val before = book.ratings
        updateBook { it.copy(ratings = it.ratings.copy(mine = stars)) }
        // A quick second tap wins – the first one is dropped, so the answers can't come back in the wrong order.
        rating?.cancel()
        rating = viewModelScope.launch {
            try {
                val reviews = repository.rate(book.id, stars)
                updateBook { it.withReviews(reviews) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Rating the book failed", e)
                updateBook { it.copy(ratings = before) }
                _ratingFailed.value = true
            }
        }
    }

    fun ratingErrorShown() {
        _ratingFailed.value = false
    }

    fun toggleFavorite() {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        val favorite = !book.isFavorite
        changeShelf(change = { it.copy(isFavorite = favorite) }) { shelf.setFavorite(book.id, favorite) }
    }

    // null takes the status back (a tap on the one already picked).
    fun setReadingStatus(status: ReadingStatus?) {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        changeShelf(change = { it.copy(readingStatus = status) }) { shelf.setStatus(book.id, status) }
    }

    fun shelfErrorShown() {
        _shelfFailed.value = false
    }

    // Like the rating: the screen changes right away, and goes back if the server doesn't take it.
    private fun changeShelf(change: (BookDetails) -> BookDetails, call: suspend () -> ShelfState) {
        val before = (_book.value as? LoadState.Ready)?.data ?: return
        updateBook(change)
        // Waits for the previous tap, so the server gets them in the same order.
        val previous = shelfChange
        shelfChange = viewModelScope.launch {
            previous?.join()
            try {
                val state = call()
                updateBook { it.copy(isFavorite = state.favorite, readingStatus = state.status) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BookWorms", "Saving the heart or reading status failed", e)
                updateBook { it.copy(isFavorite = before.isFavorite, readingStatus = before.readingStatus) }
                _shelfFailed.value = true
            }
        }
    }

    fun sendRequest(copy: BookCopy) =
        changeCopy(copy, success = CopyMessage.RequestSent(copy.ownerName)) { loans.sendRequest(copy.id) }

    fun cancelRequest(copy: BookCopy) {
        val requestId = copy.myRequestId ?: return
        changeCopy(copy) { loans.cancelRequest(requestId) }
    }

    fun joinQueue(copy: BookCopy) = changeCopy(copy) { loans.joinQueue(copy.id) }

    fun leaveQueue(copy: BookCopy) = changeCopy(copy) { loans.leaveQueue(copy.id) }

    fun copyMessageShown() {
        _copyMessage.value = null
    }

    // Sends the change, then gets "Who has it" again – also when it failed, because then someone else
    // probably changed the copy (lent it, made it unavailable…) and the screen should show how it is now.
    private fun changeCopy(copy: BookCopy, success: CopyMessage? = null, call: suspend () -> Unit) {
        val book = (_book.value as? LoadState.Ready)?.data ?: return
        // One change at a time, so two quick taps don't send two requests.
        if (_busyCopy.value != null) return
        _busyCopy.value = copy.id
        viewModelScope.launch {
            val message = try {
                call()
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: CopyChangedException) {
                CopyMessage.CopyChanged
            } catch (e: CannotAnswerException) {
                CopyMessage.CopyChanged
            } catch (e: Exception) {
                Log.w("BookWorms", "Changing the copy failed", e)
                CopyMessage.Failed
            }
            try {
                val copies = repository.copies(book.id)
                updateBook { it.withCopies(copies) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The old list stays. The message below already says something went wrong, or the change went through.
                Log.w("BookWorms", "Loading the copies failed", e)
            }
            _busyCopy.value = null
            _copyMessage.value = message
        }
    }

    private fun updateBook(change: (BookDetails) -> BookDetails) =
        _book.update { state -> if (state is LoadState.Ready) LoadState.Ready(change(state.data)) else state }
}

// What happened to an admin's change of the book.
sealed interface AdminBookEvent {
    // Edit book was saved – back to Book details, which already shows the new details.
    data object Edited : AdminBookEvent

    // The book is gone – Book details closes.
    data object Deleted : AdminBookEvent

    data object ReviewHidden : AdminBookEvent

    // The server couldn't be reached, or said no.
    data object Failed : AdminBookEvent
}

// Said in a short message after a tap on a copy's button.
sealed interface CopyMessage {
    data class RequestSent(val ownerName: String) : CopyMessage

    // Someone changed the copy in the meantime, so the tap didn't fit anymore. The list shows how it is now.
    data object CopyChanged : CopyMessage

    // The server couldn't be reached.
    data object Failed : CopyMessage
}
