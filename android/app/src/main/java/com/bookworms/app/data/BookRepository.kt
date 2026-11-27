package com.bookworms.app.data

import com.bookworms.app.BuildConfig
import com.bookworms.app.data.auth.Session
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.BookDetailsDto
import com.bookworms.app.data.network.BookReviewsDto
import com.bookworms.app.data.network.BookSummaryDto
import com.bookworms.app.data.network.BooksApi
import com.bookworms.app.data.network.CopiesApi
import com.bookworms.app.data.network.RateBookRequestDto
import com.bookworms.app.ui.book.BookComment
import com.bookworms.app.ui.book.BookDetails
import com.bookworms.app.ui.book.BookRatings
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.library.BookForm
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Duration
import java.time.Instant

// Books for the ViewModels. They don't need to know the books come from the server.
class BookRepository(
    private val api: BooksApi = ApiClient.books,
    private val copies: CopiesApi = ApiClient.copies,
    private val loans: LoansRepository = LoansRepository(),
    private val shelf: ShelfRepository = ShelfRepository(),
) {

    // Books whose title, author or ISBN contains the query. An empty query gives the newest books.
    suspend fun search(query: String): List<Book> =
        api.getBooks(query = query.trim().ifEmpty { null }).mapNotNull { it.toBook() }

    // Books the user or people in their groups have, newest first.
    suspend fun library(limit: Int): List<Book> =
        api.library(limit).mapNotNull { it.toBook() }

    // The book, its ratings, how many copies the user has and who in their groups has it, from the server.
    // TODO: group discussions – the server doesn't send them yet, so they're empty.
    suspend fun details(isbn: String): BookDetails = coroutineScope {
        val book = api.getBookByIsbn(isbn).toBookDetails(isbn)
        // Asked for at the same time, so the screen waits for the slowest one, not for all of them in a row.
        val reviews = async { api.getReviews(book.id).toBookReviews() }
        // Someone without an account has no books of their own and isn't in any group.
        val loggedIn = Session.token != null
        val own = async { if (loggedIn) copies.myCopies().filter { it.book.id == book.id } else emptyList() }
        val others = async { if (loggedIn) loans.bookCopies(book.id) else null }
        val shelfState = async { if (loggedIn) shelf.state(book.id) else ShelfState() }
        val mine = own.await()
        val state = shelfState.await()
        book.withReviews(reviews.await())
            .copy(
                ownCopies = mine.size,
                myDescription = mine.firstNotNullOfOrNull { it.description },
                isFavorite = state.favorite,
                readingStatus = state.status,
            )
            .withCopies(others.await())
    }

    // Only the ratings again, e.g. after an admin hid one.
    suspend fun reviews(bookId: String): BookReviews = api.getReviews(bookId).toBookReviews()

    // Only "Who has it" again, after the user asked for a copy or joined a queue.
    suspend fun copies(bookId: String): BookCopies = loans.bookCopies(bookId)

    // stars = 1–5, or null to take the rating back. Gives the ratings after the change.
    suspend fun rate(bookId: String, stars: Int?): BookReviews {
        val reviews = if (stars == null) api.removeRating(bookId) else api.rate(bookId, RateBookRequestDto(stars))
        return reviews.toBookReviews()
    }

    // The book's details for the Add book form ("I own this book" on Book details).
    internal suspend fun formFor(isbn: String): BookForm {
        val book = api.getBookByIsbn(isbn)
        return BookForm(
            title = book.title,
            author = book.authors.orEmpty(),
            // The form keeps digits only.
            isbn = (book.isbn ?: isbn).filter { it.isDigit() || it == 'X' },
            publisher = book.publisher.orEmpty(),
            year = book.publishedYear,
            description = book.description.orEmpty(),
        )
    }
}

// null for a book without an ISBN: the app opens book details by ISBN, so it couldn't open this one.
private fun BookSummaryDto.toBook(): Book? = isbn?.let {
    Book(
        isbn = it,
        title = title,
        author = authors.orEmpty(),
        year = publishedYear,
        coverUrl = serverUrl(coverUrl),
    )
}

// The ratings part of Book details: the numbers, and the ratings someone wrote a few words to.
data class BookReviews(val ratings: BookRatings, val comments: List<BookComment>)

fun BookDetails.withReviews(reviews: BookReviews) = copy(ratings = reviews.ratings, publicComments = reviews.comments)

fun BookDetails.withCopies(others: BookCopies?) =
    if (others == null) this else copy(groups = others.groups, copies = others.copies)

private fun BookReviewsDto.toBookReviews() = BookReviews(
    ratings = BookRatings(average = average, count = count, mine = myRating),
    comments = reviews.mapNotNull { review ->
        // Stars alone already count in the average – only ratings with words show up as comments.
        val text = review.content ?: return@mapNotNull null
        BookComment(
            id = review.id,
            authorName = review.authorName,
            authorPhotoUrl = serverUrl(review.authorPhotoUrl),
            text = text,
            daysAgo = daysSince(review.updatedAt),
            hasSpoilers = review.hasSpoilers,
            rating = review.rating,
        )
    },
)

// 0 for today. A date the app can't read counts as today too – better than not showing the comment.
private fun daysSince(time: String): Int =
    runCatching { Duration.between(Instant.parse(time), Instant.now()).toDays().toInt().coerceAtLeast(0) }
        .getOrDefault(0)

private fun BookDetailsDto.toBookDetails(requestedIsbn: String) = BookDetails(
    id = id,
    isbn = isbn ?: requestedIsbn,
    title = title,
    author = authors.orEmpty(),
    year = publishedYear,
    pages = pageCount,
    publisher = publisher,
    // The screen shows one genre.
    genre = genres.firstOrNull(),
    description = description,
    coverUrl = serverUrl(coverUrl),
    copies = emptyList(),
    publicComments = emptyList(),
    groupDiscussions = emptyList(),
)

// The server sends covers as paths on itself, e.g. "/covers/9788308075197.jpg". Images need the full address.
internal fun serverUrl(path: String?): String? = when {
    path == null -> null
    path.startsWith("http://") || path.startsWith("https://") -> path
    else -> BuildConfig.API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')
}
