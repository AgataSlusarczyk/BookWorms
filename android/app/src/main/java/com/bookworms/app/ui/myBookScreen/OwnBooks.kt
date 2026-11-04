package com.bookworms.app.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.stringResource
import com.bookworms.app.R

// One of your own books.
// groups = the groups that can see it. lentTo = the friend who has it now, or null if it's on your shelf.
internal data class OwnBook(
    val title: String,
    val author: String,
    val year: Int,
    val isbn: String,
    val publisher: String,
    val groups: List<String>,
    val lentTo: String? = null,
    val lentSince: String? = null,
    val returnBy: String? = null,
)

// TODO: the groups the user is in, from the server.
internal val myGroups = listOf("Czytelnia 23", "Rodzina")

// TODO: the user's books from the server.
// Kept outside the screens, so a book saved on Add book shows up in My books. It lasts until the app is closed.
internal val sampleOwnBooks = mutableStateListOf(
    OwnBook("Wiedźmin", "Andrzej Sapkowski", 1994, "9788375780659", "SuperNOWA", groups = myGroups),
    OwnBook(
        "Problem trzech ciał", "Liu Cixin", 2008, "9788375780638", "Rebis",
        groups = listOf("Czytelnia 23"),
        lentTo = "Marek W.", lentSince = "24.10", returnBy = "07.11",
    ),
    OwnBook("Solaris", "Stanisław Lem", 1961, "9788308049461", "Wydawnictwo Literackie", groups = myGroups),
    OwnBook(
        "Lalka", "Bolesław Prus", 1890, "9788373271897", "Greg",
        groups = listOf("Rodzina"),
        lentTo = "Ola N.", lentSince = "02.10", returnBy = "28.10",
    ),
)

// Falls back to an empty book, e.g. for a moment after the book was removed and My copy is closing.
internal fun ownBook(title: String): OwnBook =
    sampleOwnBooks.find { it.title == title } ?: OwnBook(title, "", 0, "", "", groups = emptyList())

// "Andrzej Sapkowski · 1994", or only the author when the year wasn't filled in (year = 0).
@Composable
internal fun OwnBook.authorAndYear(): String =
    if (year > 0) stringResource(R.string.book_author_year, author, year) else author
