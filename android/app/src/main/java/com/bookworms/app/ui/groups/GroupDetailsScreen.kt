package com.bookworms.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.BookRow
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.SegmentedTabs
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.PlaceholderGray
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg

// Temporary data until groups come from the server.
private data class GroupMember(val name: String, val username: String, val isAdmin: Boolean)

// borrowedBy is null while the book is on the owner's shelf.
private data class GroupBook(val isbn: String, val title: String, val author: String, val owner: String, val borrowedBy: String?)

private data class JoinRequest(val name: String, val username: String, val time: String)

private val sampleMembers = listOf(
    GroupMember("Anna Kowalska", "anna_k", isAdmin = true),
    GroupMember("Ola Nowak", "ola", isAdmin = false),
    GroupMember("Marek Wiśniewski", "marek_w", isAdmin = false),
    GroupMember("Kasia Zielińska", "kasia.z", isAdmin = false),
)

private val sampleGroupBooks = listOf(
    GroupBook("9788308049461", "Solaris", "Stanisław Lem", owner = "Ola", borrowedBy = null),
    GroupBook("9788373271897", "Lalka", "Bolesław Prus", owner = "Marek", borrowedBy = "Kasia"),
    GroupBook("9788375780638", "Problem trzech ciał", "Cixin Liu", owner = "Anna", borrowedBy = null),
)

private val sampleJoinRequests = listOf(
    JoinRequest("Tomek Lewandowski", "tomek_l", "2 h"),
)

private enum class GroupTab(val label: Int) {
    Books(R.string.tab_group_books),
    Members(R.string.tab_group_members),
}

@Composable
fun GroupDetailsScreen(
    groupName: String,
    // Only the admin sees join requests.
    isAdmin: Boolean,
    onBookClick: (isbn: String) -> Unit,
    onChatClick: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = groupName,
                onBackClick = onBackClick,
                actions = {
                    TextButton(onClick = onInviteClick) { Text(stringResource(R.string.invite_action)) }
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
            item { GroupHeader(groupName = groupName, memberCount = sampleMembers.size) }
            item { ChatCard(onClick = onChatClick) }

            if (isAdmin && sampleJoinRequests.isNotEmpty()) {
                item {
                    SectionHeader(
                        stringResource(R.string.join_requests_count, sampleJoinRequests.size),
                        Modifier.padding(top = 4.dp),
                    )
                }
                items(sampleJoinRequests, key = { it.username }) { request -> JoinRequestCard(request) }
            }

            item {
                SegmentedTabs(
                    tabs = GroupTab.entries.map { stringResource(it.label) },
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            when (GroupTab.entries[selectedTab]) {
                GroupTab.Books -> items(sampleGroupBooks, key = { it.isbn }) { book ->
                    GroupBookCard(book, onClick = { onBookClick(book.isbn) })
                }
                GroupTab.Members -> items(sampleMembers, key = { it.username }) { member ->
                    MemberCard(member)
                }
            }

            item {
                TextButton(
                    onClick = onLeaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.leave_group), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// Group picture, name, "4 members · Private" and the description.
@Composable
private fun GroupHeader(groupName: String, memberCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder(size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(groupName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(
                        R.string.group_members_visibility,
                        pluralStringResource(R.plurals.members, memberCount, memberCount),
                        stringResource(R.string.visibility_private),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // TODO: the description the admin wrote when creating the group.
        Text("Sci-fi fans from work. We meet every first Thursday.", style = MaterialTheme.typography.bodyMedium)
    }
}

// Opens the group chat. Shows the last message, so you can see if there's something new.
@Composable
private fun ChatCard(onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Icon(painterResource(R.drawable.ic_mail), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.group_chat), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Ola: Who has the new Pratchett?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Someone used the invite code and waits for the admin – the same buttons as for borrow requests.
@Composable
private fun JoinRequestCard(request: JoinRequest) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            AvatarPlaceholder()
            Spacer(Modifier.width(12.dp))
            Column {
                Text(request.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "@${request.username} · ${request.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            // TODO: send the answer to the server.
            OutlinedButton(onClick = {}, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_reject))
            }
            Button(onClick = {}, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_accept))
            }
        }
    }
}

// A book someone in the group owns: green "Available", or blue "Borrowed by X".
@Composable
private fun GroupBookCard(book: GroupBook, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        BookRow(title = book.title, subtitle = stringResource(R.string.book_by_owner, book.author, book.owner)) {
            if (book.borrowedBy == null) {
                StatusTag(stringResource(R.string.status_available), dotColor = StatusGreen, backgroundColor = StatusGreenBg, textColor = StatusGreen)
            } else {
                StatusTag(
                    stringResource(R.string.status_borrowed_by, book.borrowedBy),
                    dotColor = BrandBlue,
                    backgroundColor = StatusBlueBg,
                    textColor = BrandBlue,
                )
            }
        }
    }
}

// Picture, name, @username and the role on the right. The admin's role is blue, so it stands out.
@Composable
private fun MemberCard(member: GroupMember) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            AvatarPlaceholder()
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium)
                Text("@${member.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = stringResource(if (member.isAdmin) R.string.role_admin else R.string.role_member),
                style = MaterialTheme.typography.labelSmall,
                color = if (member.isAdmin) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(if (member.isAdmin) StatusBlueBg else PlaceholderGray, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun GroupDetailsScreenPreview() {
    BookWormsTheme {
        GroupDetailsScreen(
            groupName = "Czytelnia 23",
            isAdmin = true,
            onBookClick = {},
            onChatClick = {},
            onInviteClick = {},
            onLeaveClick = {},
            onBackClick = {},
        )
    }
}
