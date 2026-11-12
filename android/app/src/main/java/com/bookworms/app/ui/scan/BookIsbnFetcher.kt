package com.bookworms.app.ui.scan

import android.util.Log
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

suspend fun fetchBookDetailsByIsbn(
    isbn: String,
    ownerName: String,
    userGroups: List<String>,
): BookDetails = withContext(Dispatchers.IO) {
    val cleanIsbn = isbn.filter { it.isDigit() || it.equals('X', ignoreCase = true) }.ifEmpty { isbn }
    
    // Zmienne z wartościami awaryjnymi (jeśli serwer leży)
    var fetchedTitle = cleanIsbn
    var fetchedAuthor = "Unknown"
    var fetchedYear = 0
    var fetchSuccess = false

    try {
        
        // ip komputera w sieci lokalnej (np. 192.168.1.15).
        val url = URL("http://192.168.137.1:8080/api/books/lookup?isbn=$cleanIsbn")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            
            fetchedTitle = json.optString("title")
            fetchedAuthor = json.optString("author")
            fetchedYear = json.optInt("year")
            fetchSuccess = true
        }
    } catch (e: Exception) {
        Log.e("BookIsbnFetcher", "Failed to connect to backend server", e)
    }

    // Pakujemy wyniki do modelu oczekiwanego przez aplikację mobilną
    val today = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
    val userCopy = BookCopy(
        ownerName = ownerName,
        status = CopyStatus.Available(addedOn = today),
        groups = userGroups,
    )
    val emptyDiscussions = userGroups.map { GroupDiscussion(groupName = it, comments = emptyList()) }

    BookDetails(
        isbn = cleanIsbn,
        title = fetchedTitle,
        author = fetchedAuthor,
        year = fetchedYear,
        pages = 0,
        genre = "Unknown",
        description = if (fetchSuccess) "Fetched from server" else "Offline fallback",
        copies = listOf(userCopy),
        publicComments = emptyList(),
        groupDiscussions = emptyDiscussions,
    )
}