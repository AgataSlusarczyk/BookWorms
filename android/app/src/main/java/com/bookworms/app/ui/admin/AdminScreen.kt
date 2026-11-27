package com.bookworms.app.ui.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.browse.Book
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.SegmentedTabs
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.StatusAmber
import com.bookworms.app.ui.theme.StatusAmberBg
import com.bookworms.app.ui.theme.StatusBlueBg
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// The admin panel, opened from Profile. Only a system admin gets here.
// Books are edited and deleted on Book details (the Books tab leads there); reviews are hidden there too.
@Composable
fun AdminScreen(
    tab: AdminTab,
    onTabChange: (AdminTab) -> Unit,
    stats: LoadState<AdminStats>,
    users: LoadState<List<AdminUser>>,
    userQuery: String,
    onUserQueryChange: (String) -> Unit,
    groups: LoadState<List<AdminGroup>>,
    books: LoadState<List<Book>>,
    bookQuery: String,
    onBookQueryChange: (String) -> Unit,
    logs: LoadState<List<AdminLogEntry>>,
    // The account or group whose button was just tapped, while the server answers.
    busyId: String?,
    onSetBlocked: (AdminUser, Boolean) -> Unit,
    onDeleteGroup: (AdminGroup) -> Unit,
    onBookClick: (Book) -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    // What the admin is asked to confirm, kept as the id, so it survives turning the phone.
    var blockingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingGroupId by rememberSaveable { mutableStateOf<String?>(null) }

    (users as? LoadState.Ready)?.data?.find { it.id == blockingId }?.let { user ->
        ConfirmDialog(
            title = stringResource(if (user.isBlocked) R.string.admin_unblock_title else R.string.admin_block_title, user.name),
            text = stringResource(if (user.isBlocked) R.string.admin_unblock_text else R.string.admin_block_text),
            confirm = stringResource(if (user.isBlocked) R.string.admin_unblock else R.string.admin_block),
            onConfirm = {
                onSetBlocked(user, !user.isBlocked)
                blockingId = null
            },
            onDismiss = { blockingId = null },
        )
    }
    (groups as? LoadState.Ready)?.data?.find { it.id == deletingGroupId }?.let { group ->
        ConfirmDialog(
            title = stringResource(R.string.delete_group_title, group.name),
            text = stringResource(R.string.admin_delete_group_text, group.memberCount),
            confirm = stringResource(R.string.delete_action),
            onConfirm = {
                onDeleteGroup(group)
                deletingGroupId = null
            },
            onDismiss = { deletingGroupId = null },
        )
    }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.admin_panel_title), onBackClick = onBackClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                SegmentedTabs(
                    tabs = AdminTab.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = tab.ordinal,
                    onSelect = { onTabChange(AdminTab.entries[it]) },
                )
                // The search field stays above the list, so it doesn't scroll away with it.
                when (tab) {
                    AdminTab.Users -> SearchField(userQuery, onUserQueryChange, stringResource(R.string.admin_search_users))
                    AdminTab.Books -> SearchField(bookQuery, onBookQueryChange, stringResource(R.string.admin_search_books))
                    else -> Unit
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when (tab) {
                    AdminTab.Overview -> loaded(stats, onRetryClick) { overview(it) }
                    AdminTab.Users -> loaded(users, onRetryClick) { list ->
                        if (list.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_users)) }
                        items(list, key = { it.id }) { user ->
                            UserCard(user, busy = busyId == user.id, onBlockClick = { blockingId = user.id })
                        }
                    }
                    AdminTab.Groups -> loaded(groups, onRetryClick) { list ->
                        if (list.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_groups)) }
                        items(list, key = { it.id }) { group ->
                            GroupCard(group, busy = busyId == group.id, onDeleteClick = { deletingGroupId = group.id })
                        }
                    }
                    AdminTab.Books -> loaded(books, onRetryClick) { list ->
                        item { EmptyText(stringResource(R.string.admin_books_hint)) }
                        if (list.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_books)) }
                        items(list, key = { it.isbn }) { book ->
                            AppCard(onClick = { onBookClick(book) }) {
                                BookRow(
                                    title = book.title,
                                    subtitle = listOfNotNull(book.author.ifEmpty { null }, book.year?.toString()).joinToString(" · "),
                                    coverUrl = book.coverUrl,
                                )
                            }
                        }
                    }
                    AdminTab.Log -> loaded(logs, onRetryClick) { list ->
                        if (list.isEmpty()) item { EmptyText(stringResource(R.string.admin_no_logs)) }
                        items(list, key = { it.id }) { LogCard(it) }
                    }
                }
            }
        }
    }
}

// A spinning circle or "Try again" until the tab's data is there, then content.
private fun <T> LazyListScope.loaded(state: LoadState<T>, onRetryClick: () -> Unit, content: LazyListScope.(T) -> Unit) {
    when (state) {
        LoadState.Loading -> item { LoadingIndicator() }
        LoadState.Failed -> item {
            LoadFailedMessage(stringResource(R.string.admin_load_failed), onRetryClick)
        }
        is LoadState.Ready -> content(state.data)
    }
}

@StringRes
private fun AdminTab.labelRes(): Int = when (this) {
    AdminTab.Overview -> R.string.admin_tab_overview
    AdminTab.Users -> R.string.admin_tab_users
    AdminTab.Groups -> R.string.admin_tab_groups
    AdminTab.Books -> R.string.admin_tab_books
    AdminTab.Log -> R.string.admin_tab_log
}

// Two tiles in a row: the number big, what it counts under it.
private fun LazyListScope.overview(stats: AdminStats) {
    val tiles = listOf(
        Triple(stats.users, R.string.admin_stat_users, stats.blockedUsers.takeIf { it > 0 }),
        Triple(stats.books, R.string.admin_stat_books, null),
        Triple(stats.copies, R.string.admin_stat_copies, null),
        Triple(stats.groups, R.string.admin_stat_groups, null),
        Triple(stats.reviews, R.string.admin_stat_reviews, null),
        Triple(stats.activeLoans, R.string.admin_stat_loans, null),
    )
    items(tiles.chunked(2)) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (number, label, blocked) ->
                StatTile(number, stringResource(label), blocked, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(number: Int, label: String, blocked: Int?, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(number.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (blocked != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    pluralStringResource(R.plurals.admin_blocked_count, blocked, blocked),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = FieldShape,
        colors = authFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    )
}

// Photo, name, "@username · email", a label for admins and blocked accounts, and Block / Unblock on the right.
@Composable
private fun UserCard(user: AdminUser, busy: Boolean, onBlockClick: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            ProfilePhoto(photo = user.photoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${user.username} · ${user.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.admin_user_joined, user.joined.format(Date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    user.isAdmin -> Pill(
                        stringResource(R.string.admin_role_admin),
                        containerColor = StatusBlueBg,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    user.isBlocked -> Pill(
                        stringResource(R.string.admin_status_blocked),
                        containerColor = StatusAmberBg,
                        contentColor = StatusAmber,
                        showDot = true,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
            // Another admin can't be blocked – the server would say no anyway.
            if (!user.isAdmin) {
                TextButton(onClick = onBlockClick, enabled = !busy) {
                    Text(
                        stringResource(if (user.isBlocked) R.string.admin_unblock else R.string.admin_block),
                        color = if (busy) Color.Unspecified else if (user.isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: AdminGroup, busy: Boolean, onDeleteClick: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            ProfilePhoto(photo = group.photoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluralStringResource(R.plurals.members, group.memberCount, group.memberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.admin_group_created_by, group.createdByName, group.created.format(Date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onDeleteClick, enabled = !busy) {
                Text(
                    stringResource(R.string.delete_action),
                    color = if (busy) Color.Unspecified else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// e.g. "Deleted a book", "Solaris", "Admin · 27.11.2026 10:15".
@Composable
private fun LogCard(entry: AdminLogEntry) {
    AppCard {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(entry.action.labelRes()), style = MaterialTheme.typography.titleSmall)
            entry.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(2.dp))
            Text(
                "${entry.adminName} · ${entry.time.format(DateTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun AdminAction.labelRes(): Int = when (this) {
    AdminAction.BLOCK_USER -> R.string.admin_log_block_user
    AdminAction.UNBLOCK_USER -> R.string.admin_log_unblock_user
    AdminAction.EDIT_BOOK -> R.string.admin_log_edit_book
    AdminAction.DELETE_BOOK -> R.string.admin_log_delete_book
    AdminAction.HIDE_REVIEW -> R.string.admin_log_hide_review
    AdminAction.DELETE_GROUP -> R.string.admin_log_delete_group
    AdminAction.OTHER -> R.string.admin_log_other
}

@Composable
private fun EmptyText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val Date = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val DateTime = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Preview(device = "id:pixel_8")
@Composable
private fun AdminScreenPreview() {
    BookWormsTheme {
        AdminScreen(
            tab = AdminTab.Users,
            onTabChange = {},
            stats = LoadState.Loading,
            users = LoadState.Ready(
                listOf(
                    AdminUser("1", "Admin", "admin", "admin@bookworms.dev", null, isAdmin = true, isBlocked = false, LocalDate.of(2026, 9, 1)),
                    AdminUser("2", "Anna Kowalska", "anna_k", "anna@mail.com", null, isAdmin = false, isBlocked = true, LocalDate.of(2026, 10, 3)),
                    AdminUser("3", "Marek W.", "marek", "marek@mail.com", null, isAdmin = false, isBlocked = false, LocalDate.of(2026, 11, 2)),
                ),
            ),
            userQuery = "",
            onUserQueryChange = {},
            groups = LoadState.Loading,
            books = LoadState.Loading,
            bookQuery = "",
            onBookQueryChange = {},
            logs = LoadState.Ready(listOf(AdminLogEntry("l1", "Admin", AdminAction.DELETE_BOOK, "Solaris", LocalDateTime.now()))),
            busyId = null,
            onSetBlocked = { _, _ -> },
            onDeleteGroup = {},
            onBookClick = {},
            onRetryClick = {},
            onBackClick = {},
        )
    }
}
