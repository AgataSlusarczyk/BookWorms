package com.bookworms.app.ui.scan

import com.bookworms.app.ui.book.BookCopy
import com.bookworms.app.ui.book.BookDetails
import com.bookworms.app.ui.book.CopyStatus
import com.bookworms.app.ui.book.GroupDiscussion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun fetchBookDetailsByIsbn(
    isbn: String,
    ownerName: String,
    userGroups: List<String>,
): BookDetails = withContext(Dispatchers.IO) {
    val cleanIsbn = isbn.filter { it.isDigit() }.ifEmpty { isbn }
    val today = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
    val userCopy = BookCopy(
        ownerName = ownerName,
        status = CopyStatus.Available(addedOn = today),
        groups = userGroups,
    )
    val emptyDiscussions = userGroups.map { GroupDiscussion(groupName = it, comments = emptyList()) }

    BookDetails(
        isbn = cleanIsbn,
        title = cleanIsbn,
        author = "Unknown",
        year = 0,
        pages = 0,
        genre = "Unknown",
        description = "Unknown",
        copies = listOf(userCopy),
        publicComments = emptyList(),
        groupDiscussions = emptyDiscussions,
    )
}