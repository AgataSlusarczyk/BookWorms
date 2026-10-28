package com.bookworms.app.ui.browse

// One book in the search results.
data class Book(
    val isbn: String,
    val title: String,
    val author: String,
    val year: Int,
    // In how many of the user's groups someone has a copy. 0 = nobody has it.
    val groupsAvailable: Int,
)

// Temporary data until the app gets books from the server.
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
