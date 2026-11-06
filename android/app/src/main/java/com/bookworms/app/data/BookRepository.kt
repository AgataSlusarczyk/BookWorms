package com.bookworms.app.data

import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.data.network.BookDetailsDto
import com.bookworms.app.data.network.BookSummaryDto
import com.bookworms.app.data.network.BooksApi
import com.bookworms.app.ui.book.BookDetails
import com.bookworms.app.ui.book.withSampleCopiesAndComments
import com.bookworms.app.ui.browse.Book

// Books for the ViewModels. They don't need to know the books come from the server.
class BookRepository(private val api: BooksApi = ApiClient.books) {

    // Books whose title, author or ISBN contains the query. An empty query gives the newest books.
    suspend fun search(query: String): List<Book> =
        api.getBooks(query = query.trim().ifEmpty { null }).mapNotNull { it.toBook() }

    suspend fun newest(limit: Int): List<Book> =
        api.getBooks(limit = limit).mapNotNull { it.toBook() }

    // The book from the server. Copies and comments are still sample data (see withSampleCopiesAndComments).
    suspend fun details(isbn: String): BookDetails =
        withSampleCopiesAndComments(api.getBookByIsbn(isbn).toBookDetails(isbn))
}

// null for a book without an ISBN: the app opens book details by ISBN, so it couldn't open this one.
private fun BookSummaryDto.toBook(): Book? = isbn?.let {
    Book(
        isbn = it,
        title = title,
        author = authors.orEmpty(),
        year = publishedYear,
    )
}

private fun BookDetailsDto.toBookDetails(requestedIsbn: String) = BookDetails(
    isbn = isbn ?: requestedIsbn,
    title = title,
    author = authors.orEmpty(),
    year = publishedYear,
    pages = pageCount,
    // The screen shows one genre.
    genre = genres.firstOrNull(),
    description = description,
    copies = emptyList(),
    publicComments = emptyList(),
    groupDiscussions = emptyList(),
)
