package com.bookworms.app.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookworms.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// One of your own books (a copy on the server).
// lentTo = the friend who has it now, or null if it's on your shelf.
internal data class OwnBook(
    // The copy's id on the server.
    val id: String,
    val title: String,
    val author: String,
    val year: Int? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val coverUrl: String? = null,
    // false = the owner turned off "Available to borrow" (or marked the book as lost).
    val availableToBorrow: Boolean = true,
    // true = the user typed the book in by hand, so they can edit it. Catalog books are shared, so they can't.
    val canEditBook: Boolean = false,
    // The user's groups that can see it ("Visible in" on My copy).
    val groupIds: Set<String> = emptySet(),
    val lentTo: String? = null,
    val lentSince: LocalDate? = null,
    val returnBy: LocalDate? = null,
)

// What the Add book and Edit book form holds. Empty text = not filled in.
internal data class BookForm(
    val title: String = "",
    val author: String = "",
    // Digits only (and an X at the end of old 10-digit ones).
    val isbn: String = "",
    val publisher: String = "",
    val year: Int? = null,
)

internal fun OwnBook.toForm() = BookForm(
    title = title,
    author = author,
    isbn = isbn.orEmpty(),
    publisher = publisher.orEmpty(),
    year = year,
)

// "Andrzej Sapkowski · 1994", or only the author when the year wasn't filled in.
@Composable
internal fun OwnBook.authorAndYear(): String =
    if (year != null) stringResource(R.string.book_author_year, author, year) else author

// "24.10" – the short date on the loan lines.
internal fun LocalDate.shortDate(): String = format(ShortDate)

private val ShortDate = DateTimeFormatter.ofPattern("dd.MM")

// For the previews.
internal val previewOwnBooks = listOf(
    OwnBook("1", "Wiedźmin", "Andrzej Sapkowski", 1994, "9788375780659", "SuperNOWA"),
    OwnBook(
        "2", "Problem trzech ciał", "Liu Cixin", 2008, "9788381881548", "Rebis",
        lentTo = "Marek W.", lentSince = LocalDate.of(2026, 10, 24), returnBy = LocalDate.of(2026, 11, 7),
    ),
    OwnBook("3", "Solaris", "Stanisław Lem", 1961, "9788308075197", "Wydawnictwo Literackie", availableToBorrow = false),
    OwnBook("4", "Moja książka", "Ja", canEditBook = true),
)
