package com.bookworms.app.ui.book

// Everything the book details screen shows about one book.
data class BookDetails(
    val isbn: String,
    val title: String,
    val author: String,
    val year: Int,
    val pages: Int,
    val genre: String,
    val description: String,
    // Copies that people in the user's groups own.
    val copies: List<BookCopy>,
    val comments: List<BookComment>,
)

data class BookCopy(
    val ownerName: String,
    val status: CopyStatus,
)

sealed interface CopyStatus {
    // On the owner's shelf, so it can be borrowed right away.
    data class Available(val addedOn: String) : CopyStatus

    // Someone has it now. queuePosition = the user's place in the queue after joining it.
    data class Borrowed(val returnBy: String, val queuePosition: Int) : CopyStatus
}

data class BookComment(
    val authorName: String,
    val text: String,
    val daysAgo: Int,
)

// What the user picked under "Your reading status".
enum class ReadingStatus { TO_READ, IN_PROGRESS, FINISHED }

// Temporary data until the app gets books from the server.
// The dates are plain text for now; they'll be real dates once they come from the server.
val sampleBookDetails = BookDetails(
    isbn = "978-83-08-04815-3",
    title = "Solaris",
    author = "Stanisław Lem",
    year = 1961,
    pages = 224,
    genre = "science fiction",
    description = "Stacja badawcza nad oceanem planety, który odpowiada na ludzkie pytania " +
        "w sposób niemożliwy do rozszyfrowania.",
    copies = listOf(
        BookCopy("Anna K.", CopyStatus.Available(addedOn = "12.09")),
        BookCopy("Marek W.", CopyStatus.Borrowed(returnBy = "20.11", queuePosition = 2)),
    ),
    comments = listOf(
        BookComment("Marek W.", "Pierwsze 50 stron się wlecze, potem nie da się odłożyć.", daysAgo = 3),
        BookComment("Anna K.", "Lepsze niż film. Zakończenie zostaje w głowie na długo.", daysAgo = 8),
    ),
)
