package com.bookworms.app.ui.library

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
internal val sampleOwnBooks = listOf(
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

internal fun ownBook(title: String): OwnBook = sampleOwnBooks.find { it.title == title } ?: sampleOwnBooks.first()
