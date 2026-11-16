package com.bookworms.app.data

import android.content.Context
import android.util.Log
import com.bookworms.app.data.network.CopyDto
import com.bookworms.app.data.network.LoanDto
import com.bookworms.app.data.network.RequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

// The last answers from the server about the user's books and loans, kept on the phone,
// so My books, Library and Loans can still show them when the server can't be reached.
// Only to look at: nothing can be changed without the server.
// Each file remembers whose data it is, and logging out deletes them all (Session.end).
object SavedData {

    private lateinit var dir: File

    private val json = Json { ignoreUnknownKeys = true }

    // Called once, when the app starts (BookWormsApp). In the app's own folder – other apps can't read it.
    fun init(context: Context) {
        dir = File(context.filesDir, "saved")
    }

    internal suspend fun saveMyBooks(userId: String, books: SavedMyBooks) =
        write(MyBooksFile, Saved.serializer(SavedMyBooks.serializer()), Saved(userId, books))

    internal suspend fun myBooks(userId: String): SavedMyBooks? =
        read(MyBooksFile, Saved.serializer(SavedMyBooks.serializer()))?.takeIf { it.userId == userId }?.data

    internal suspend fun saveLoans(userId: String, loans: SavedLoans) =
        write(LoansFile, Saved.serializer(SavedLoans.serializer()), Saved(userId, loans))

    internal suspend fun loans(userId: String): SavedLoans? =
        read(LoansFile, Saved.serializer(SavedLoans.serializer()))?.takeIf { it.userId == userId }?.data

    // Logging out: the next person on this phone mustn't see the books and loans.
    fun clear() {
        if (::dir.isInitialized) dir.deleteRecursively()
    }

    // A failed save only means there's no copy for later – the screen still shows the fresh data.
    private suspend fun <T> write(name: String, serializer: KSerializer<T>, value: T) = withContext(Dispatchers.IO) {
        try {
            dir.mkdirs()
            File(dir, name).writeText(json.encodeToString(serializer, value))
        } catch (e: Exception) {
            Log.w("BookWorms", "Saving $name on the phone failed", e)
        }
    }

    // null = nothing saved yet, or a file the app can't read anymore (e.g. saved by an older version).
    private suspend fun <T> read(name: String, serializer: KSerializer<T>): T? = withContext(Dispatchers.IO) {
        try {
            val file = File(dir, name)
            if (file.exists()) json.decodeFromString(serializer, file.readText()) else null
        } catch (e: Exception) {
            Log.w("BookWorms", "Reading $name from the phone failed", e)
            null
        }
    }

    private const val MyBooksFile = "my_books.json"
    private const val LoansFile = "loans.json"
}

@Serializable
internal data class Saved<T>(val userId: String, val data: T)

// Kept just as the server sent them, so they're turned into screen data the same way as fresh ones.
@Serializable
internal data class SavedMyBooks(val copies: List<CopyDto>, val lent: List<LoanDto>)

@Serializable
internal data class SavedLoans(
    val borrowed: List<LoanDto>,
    val sent: List<RequestDto>,
    val lent: List<LoanDto>,
    val received: List<RequestDto>,
)
