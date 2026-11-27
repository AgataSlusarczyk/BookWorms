package com.bookworms.app.ui.admin

import java.time.LocalDate
import java.time.LocalDateTime

// What the admin panel shows. Only a system admin sees any of it.

// The numbers on the Overview tab.
data class AdminStats(
    val users: Int,
    val blockedUsers: Int,
    val books: Int,
    val copies: Int,
    val groups: Int,
    val reviews: Int,
    val activeLoans: Int,
)

// One account on the Users tab.
data class AdminUser(
    val id: String,
    val name: String,
    val username: String,
    val email: String,
    // Full address of the photo, or null for the gray circle.
    val photoUrl: String?,
    // Another admin can't be blocked, so there's no button for them.
    val isAdmin: Boolean,
    val isBlocked: Boolean,
    val joined: LocalDate,
)

// One group on the Groups tab – also groups the admin isn't in.
data class AdminGroup(
    val id: String,
    val name: String,
    val description: String?,
    val photoUrl: String?,
    val memberCount: Int,
    val createdByName: String,
    val created: LocalDate,
)

// One line on the Log tab: who did what, and when.
data class AdminLogEntry(
    val id: String,
    val adminName: String,
    val action: AdminAction,
    // What it was about, e.g. the deleted book's title or "@username".
    val description: String?,
    val time: LocalDateTime,
)

// The same names as on the server. OTHER = something a newer server does that this app doesn't know yet.
enum class AdminAction { BLOCK_USER, UNBLOCK_USER, EDIT_BOOK, DELETE_BOOK, HIDE_REVIEW, DELETE_GROUP, OTHER }

// The panel's tabs, in order.
enum class AdminTab { Overview, Users, Groups, Books, Log }
