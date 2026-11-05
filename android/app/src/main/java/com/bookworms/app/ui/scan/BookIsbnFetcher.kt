package com.bookworms.app.ui.scan

import com.bookworms.app.ui.book.BookCopy
import com.bookworms.app.ui.book.BookDetails
import com.bookworms.app.ui.book.CopyStatus
import com.bookworms.app.ui.book.GroupDiscussion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Fetches book metadata by ISBN from the server (OpenLibrary API) and creates a BookDetails
// instance with the current user's available copy attached.
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

    try {
        val url = URL("https://openlibrary.org/api/books?bibkeys=ISBN:$cleanIsbn&format=json&jscmd=data")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(response)
            val bookJson = root.optJSONObject("ISBN:$cleanIsbn")

            if (bookJson != null) {
                val title = bookJson.optString("title").ifBlank { "Book $cleanIsbn" }
                val authorsArray = bookJson.optJSONArray("authors")
                val author = if (authorsArray != null && authorsArray.length() > 0) {
                    authorsArray.getJSONObject(0).optString("name", "Unknown author")
                } else {
                    "Unknown author"
                }
                val publishDate = bookJson.optString("publish_date", "2024")
                val year = Regex("\\d{4}").find(publishDate)?.value?.toIntOrNull() ?: 2024
                val pages = bookJson.optInt("number_of_pages", 250).takeIf { it > 0 } ?: 250

                val subjectsArray = bookJson.optJSONArray("subjects")
                val genre = if (subjectsArray != null && subjectsArray.length() > 0) {
                    subjectsArray.getJSONObject(0).optString("name", "fiction").lowercase()
                } else {
                    "fiction"
                }

                val notes = when (val rawNotes = bookJson.opt("notes")) {
                    is String -> rawNotes
                    is JSONObject -> rawNotes.optString("value", "")
                    else -> ""
                }
                val subtitle = bookJson.optString("subtitle", "")
                val description = when {
                    notes.isNotBlank() -> notes
                    subtitle.isNotBlank() -> subtitle
                    else -> "Added via ISBN scanner ($cleanIsbn)."
                }

                return@withContext BookDetails(
                    isbn = cleanIsbn,
                    title = title,
                    author = author,
                    year = year,
                    pages = pages,
                    genre = genre,
                    description = description,
                    copies = listOf(userCopy),
                    publicComments = emptyList(),
                    groupDiscussions = emptyDiscussions,
                )
            }
        }
    } catch (_: Exception) {
        // Fallback when offline or server is unreachable.
    }

    BookDetails(
        isbn = cleanIsbn,
        title = "Scanned book ($cleanIsbn)",
        author = "Unknown author",
        year = 2026,
        pages = 200,
        genre = "general",
        description = "Book added by scanning ISBN $cleanIsbn.",
        copies = listOf(userCopy),
        publicComments = emptyList(),
        groupDiscussions = emptyDiscussions,
    )
}