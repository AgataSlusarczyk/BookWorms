package com.bookworms.app.ui.book

import com.bookworms.app.ui.browse.Book

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
    // Everyone sees these, even people without an account.
    val publicComments: List<BookComment>,
    // One for each of the user's groups. Only people in that group see it.
    val groupDiscussions: List<GroupDiscussion>,
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
    val id: String,
    val authorName: String,
    val text: String,
    val daysAgo: Int,
    // The text stays hidden until someone taps it.
    val hasSpoilers: Boolean = false,
    // Answers under the comment. Only group discussions have them – public comments don't.
    val replies: List<BookComment> = emptyList(),
)

// What one group says about the book.
data class GroupDiscussion(
    val groupName: String,
    val comments: List<BookComment>,
)

// Looks for a comment or a reply with this id in all the group discussions.
fun BookDetails.findGroupComment(id: String): BookComment? =
    groupDiscussions.flatMap { it.comments }.find { it.id == id }

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
    publicComments = listOf(
        BookComment("p1", "Marek W.", "Pierwsze 50 stron się wlecze, potem nie da się odłożyć.", daysAgo = 3),
        BookComment("p2", "Anna K.", "Lepsze niż film. Zakończenie zostaje w głowie na długo.", daysAgo = 8),
        BookComment("p3", "Tomek L.", "Kelvin w końcu zostaje na stacji, bo nie potrafi zostawić Harey.", daysAgo = 12, hasSpoilers = true),
    ),
    groupDiscussions = listOf(
        GroupDiscussion(
            groupName = "Czytelnia 23",
            comments = listOf(
                BookComment(
                    id = "g1",
                    authorName = "Ola N.",
                    text = "Czytamy to na czwartek? Jestem w połowie i nie wiem, co myśleć o oceanie.",
                    daysAgo = 2,
                    replies = listOf(
                        BookComment("g1r1", "Marek W.", "Tak, na czwartek. Ocean to cała zagadka – Lem celowo nic nie wyjaśnia.", daysAgo = 2),
                        BookComment("g1r2", "Anna K.", "Poczekaj do rozdziału o Harey, wtedy wszystko się zmienia.", daysAgo = 1, hasSpoilers = true),
                    ),
                ),
            ),
        ),
        GroupDiscussion(groupName = "Rodzina", comments = emptyList()),
    ),
)

// Temporary: the sample details above, but with the title, author, year and ISBN
// of the book the user tapped, so each book in the list opens its own page.
fun sampleBookDetailsFor(book: Book) = sampleBookDetails.copy(
    isbn = book.isbn,
    title = book.title,
    author = book.author,
    year = book.year,
)
