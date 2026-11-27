package com.bookworms.app.data

import com.bookworms.app.data.network.AdminApi
import com.bookworms.app.data.network.AdminEditBookRequestDto
import com.bookworms.app.data.network.AdminGroupDto
import com.bookworms.app.data.network.AdminLogDto
import com.bookworms.app.data.network.AdminStatsDto
import com.bookworms.app.data.network.AdminUserDto
import com.bookworms.app.data.network.ApiClient
import com.bookworms.app.ui.admin.AdminAction
import com.bookworms.app.ui.admin.AdminGroup
import com.bookworms.app.ui.admin.AdminLogEntry
import com.bookworms.app.ui.admin.AdminStats
import com.bookworms.app.ui.admin.AdminUser
import com.bookworms.app.ui.library.BookForm
import retrofit2.HttpException
import retrofit2.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// The admin panel, and the admin's buttons on Book details. The server checks on every call that the user is an admin.
class AdminRepository(private val api: AdminApi = ApiClient.admin) {

    suspend fun stats(): AdminStats = api.stats().toStats()

    // An empty query gives the newest accounts.
    suspend fun users(query: String): List<AdminUser> =
        api.users(query.trim().ifEmpty { null }).map { it.toUser() }

    // Throws CannotBlockException for the admin themselves or another admin, and anything else when the server can't be reached.
    suspend fun setBlocked(userId: String, blocked: Boolean): AdminUser {
        val user = try {
            if (blocked) api.block(userId) else api.unblock(userId)
        } catch (e: HttpException) {
            throw if (e.code() == 409) CannotBlockException() else e
        }
        return user.toUser()
    }

    suspend fun groups(): List<AdminGroup> = api.groups().map { it.toGroup() }

    suspend fun deleteGroup(groupId: String) = api.deleteGroup(groupId).orThrow()

    // Changes the book for everyone. The ISBN on the form is left out – the server doesn't change it.
    internal suspend fun editBook(bookId: String, form: BookForm) {
        api.editBook(
            bookId,
            AdminEditBookRequestDto(
                title = form.title.trim(),
                authors = form.author.trim(),
                publisher = form.publisher.trim().ifEmpty { null },
                publishedYear = form.year,
                description = form.description.trim().ifEmpty { null },
            ),
        )
    }

    suspend fun deleteBook(bookId: String) = api.deleteBook(bookId).orThrow()

    suspend fun hideReview(reviewId: String) = api.hideReview(reviewId).orThrow()

    suspend fun logs(): List<AdminLogEntry> = api.logs().map { it.toEntry() }

    private fun Response<Unit>.orThrow() {
        if (!isSuccessful) throw HttpException(this)
    }
}

// The server said no: that's the admin themselves, or another admin.
class CannotBlockException : Exception("This account can't be blocked")

private fun AdminStatsDto.toStats() = AdminStats(
    users = users,
    blockedUsers = blockedUsers,
    books = books,
    copies = copies,
    groups = groups,
    reviews = reviews,
    activeLoans = activeLoans,
)

private fun AdminUserDto.toUser() = AdminUser(
    id = id,
    name = name,
    username = username,
    email = email,
    photoUrl = serverUrl(photoUrl),
    isAdmin = role == "SYSTEM_ADMIN",
    isBlocked = status == "BLOCKED",
    joined = localTime(createdAt).toLocalDate(),
)

private fun AdminGroupDto.toGroup() = AdminGroup(
    id = id,
    name = name,
    description = description,
    photoUrl = serverUrl(photoUrl),
    memberCount = memberCount,
    createdByName = createdBy.name,
    created = localTime(createdAt).toLocalDate(),
)

private fun AdminLogDto.toEntry() = AdminLogEntry(
    id = id,
    adminName = admin.name,
    action = AdminAction.entries.find { it.name == action } ?: AdminAction.OTHER,
    description = description,
    time = localTime(createdAt),
)

// The server sends times in UTC, e.g. "2026-11-14T20:15:00Z". A time the app can't read counts as now.
private fun localTime(time: String): LocalDateTime =
    runCatching { LocalDateTime.ofInstant(Instant.parse(time), ZoneId.systemDefault()) }.getOrElse { LocalDateTime.now() }
