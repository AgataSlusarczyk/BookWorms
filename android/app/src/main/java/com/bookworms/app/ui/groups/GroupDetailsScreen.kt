package com.bookworms.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.SegmentedTabs
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.PlaceholderGray
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class GroupTab(val label: Int) {
    Books(R.string.tab_group_books),
    Members(R.string.tab_group_members),
}

private val JoinedDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
internal fun GroupDetailsScreen(
    group: GroupDetails,
    books: LoadState<List<GroupBook>>,
    // true while a change is on its way to the server – the buttons wait for it.
    saving: Boolean,
    onRetryBooksClick: () -> Unit,
    onBookClick: (isbn: String) -> Unit,
    // null = the user can't invite (an ordinary member, and the admin didn't allow it).
    onInviteClick: (() -> Unit)?,
    onEditClick: () -> Unit,
    onMakeAdmin: (GroupMember) -> Unit,
    onRemoveMember: (GroupMember) -> Unit,
    onLeaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    // Who the "Remove from group?" dialog asks about (their id), or null when it's closed.
    var memberToRemove by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = group.name,
                onBackClick = onBackClick,
                actions = {
                    if (group.isAdmin) {
                        IconButton(onClick = onEditClick) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_group_title))
                        }
                    }
                    if (onInviteClick != null) {
                        TextButton(onClick = onInviteClick) { Text(stringResource(R.string.invite_action)) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item { GroupHeader(group) }

            item {
                SegmentedTabs(
                    tabs = GroupTab.entries.map { stringResource(it.label) },
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            when (GroupTab.entries[selectedTab]) {
                GroupTab.Books -> when (books) {
                    LoadState.Loading -> item { LoadingIndicator() }
                    LoadState.Failed -> item { LoadFailedMessage(stringResource(R.string.load_books_failed), onRetryBooksClick) }
                    is LoadState.Ready -> {
                        if (books.data.isEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.group_books_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(books.data, key = { it.copyId }) { book ->
                            GroupBookCard(book, onClick = book.isbn?.let { isbn -> { onBookClick(isbn) } })
                        }
                    }
                }
                GroupTab.Members -> items(group.members, key = { it.id }) { member ->
                    MemberCard(
                        member,
                        // The admin can manage members, but not other admins.
                        canManage = group.isAdmin && !member.isAdmin && !saving,
                        onMakeAdmin = { onMakeAdmin(member) },
                        onRemove = { memberToRemove = member.id },
                    )
                }
            }

            item {
                TextButton(
                    onClick = { confirmLeave = true },
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.leave_group), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Asks first, so nobody gets removed by a slip of the finger.
    group.members.find { it.id == memberToRemove }?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.remove_member_title, member.name, group.name)) },
            text = { Text(stringResource(R.string.remove_member_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        memberToRemove = null
                        onRemoveMember(member)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Leaving takes the user's books out of the group, so it asks first too.
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.leave_group_title, group.name)) },
            text = { Text(stringResource(R.string.leave_group_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onLeaveClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.leave_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

// Group picture, name, "4 members" and the description.
@Composable
private fun GroupHeader(group: GroupDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder(size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = pluralStringResource(R.plurals.members, group.members.size, group.members.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (group.description.isNotBlank()) {
            Text(group.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// A book someone in the group owns: blue "Borrowed by X", green "Available" or gray "Not available".
@Composable
private fun GroupBookCard(book: GroupBook, onClick: (() -> Unit)?) {
    AppCard(onClick = onClick) {
        BookRow(
            title = book.title,
            subtitle = stringResource(R.string.book_by_owner, book.author, book.owner),
            coverUrl = book.coverUrl,
        ) {
            when {
                book.borrowedBy != null -> StatusTag(
                    stringResource(R.string.status_borrowed_by, book.borrowedBy),
                    dotColor = BrandBlue,
                    backgroundColor = StatusBlueBg,
                    textColor = BrandBlue,
                )
                book.available -> StatusTag(
                    stringResource(R.string.status_available),
                    dotColor = StatusGreen,
                    backgroundColor = StatusGreenBg,
                    textColor = StatusGreen,
                )
                else -> StatusTag(stringResource(R.string.status_not_available))
            }
        }
    }
}

// Picture, name, when they joined and the role on the right. The admin's role is blue, so it stands out.
// With canManage there's also a ⋮ menu: Make admin and Remove from group.
@Composable
private fun MemberCard(member: GroupMember, canManage: Boolean, onMakeAdmin: () -> Unit, onRemove: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            AvatarPlaceholder()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.member_joined, member.joined.format(JoinedDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(if (member.isAdmin) R.string.role_admin else R.string.role_member),
                style = MaterialTheme.typography.labelSmall,
                color = if (member.isAdmin) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(if (member.isAdmin) StatusBlueBg else PlaceholderGray, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            if (canManage) MemberMenu(member.name, onMakeAdmin, onRemove)
        }
    }
}

// ⋮ button with the admin's options for one member. Box, so the menu opens right under the button.
@Composable
private fun MemberMenu(memberName: String, onMakeAdmin: () -> Unit, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.member_options, memberName))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.make_admin)) },
                onClick = {
                    expanded = false
                    onMakeAdmin()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_group), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onRemove()
                },
            )
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun GroupDetailsScreenPreview() {
    val today = LocalDate.now()
    BookWormsTheme {
        GroupDetailsScreen(
            group = GroupDetails(
                id = "1",
                name = "Czytelnia 23",
                description = "Sci-fi fans from work. We meet every first Thursday.",
                isAdmin = true,
                membersCanInvite = false,
                inviteCode = "K7QM-3XTP",
                members = listOf(
                    GroupMember("a", "Anna Kowalska", isAdmin = true, joined = today.minusDays(40)),
                    GroupMember("o", "Ola Nowak", isAdmin = false, joined = today.minusDays(3)),
                ),
            ),
            books = LoadState.Ready(
                listOf(
                    GroupBook("c1", null, "Solaris", "Stanisław Lem", null, owner = "Ola", borrowedBy = null, available = true),
                    GroupBook("c2", null, "Lalka", "Bolesław Prus", null, owner = "Anna", borrowedBy = "Ola", available = true),
                ),
            ),
            saving = false,
            onRetryBooksClick = {},
            onBookClick = {},
            onInviteClick = {},
            onEditClick = {},
            onMakeAdmin = {},
            onRemoveMember = {},
            onLeaveClick = {},
            onBackClick = {},
        )
    }
}
