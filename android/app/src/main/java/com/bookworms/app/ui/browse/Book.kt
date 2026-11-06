package com.bookworms.app.ui.browse

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookworms.app.R

// One book in the search results.
data class Book(
    val isbn: String,
    val title: String,
    val author: String,
    // null when the server doesn't know it.
    val year: Int?,
    // In how many of the user's groups someone has a copy. 0 = nobody has it.
    // null = not known: the catalog doesn't show copies (guests, and until we have groups on the server).
    val groupsAvailable: Int? = null,
)

// "Stanisław Lem · 1961", or only the author when the year isn't known.
@Composable
fun Book.authorAndYear(): String =
    year?.let { stringResource(R.string.book_author_year, author, it) } ?: author

// Sample books for previews and for the screens that don't use the server yet.
val sampleBooks = listOf(
    Book("9788308049461", "Solaris", "Stanisław Lem", 1961, groupsAvailable = 2),
    Book("9788308057619", "Niezwyciężony", "Stanisław Lem", 1964, groupsAvailable = 1),
    Book("9788308060411", "Opowieści o pilocie Pirxie", "Stanisław Lem", 1968, groupsAvailable = 0),
    Book("9788373271897", "Lalka", "Bolesław Prus", 1890, groupsAvailable = 3),
    Book("9788375780638", "Problem trzech ciał", "Cixin Liu", 2008, groupsAvailable = 1),
)

// Books whose title, author or ISBN contains the query. Upper/lower case doesn't matter,
// and dashes or spaces in an ISBN are skipped, so "978-83-08" also works.
fun filterBooks(books: List<Book>, query: String): List<Book> {
    val text = query.trim()
    if (text.isEmpty()) return books

    // Only digits, dashes and spaces means the user typed (part of) an ISBN.
    val digits = text.filter { it.isDigit() }
    val looksLikeIsbn = digits.isNotEmpty() && text.all { it.isDigit() || it == '-' || it == ' ' }

    return books.filter { book ->
        book.title.contains(text, ignoreCase = true) ||
            book.author.contains(text, ignoreCase = true) ||
            (looksLikeIsbn && book.isbn.contains(digits))
    }
}
