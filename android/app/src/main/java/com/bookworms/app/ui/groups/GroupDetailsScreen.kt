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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    // The name from the Groups list. The name shown comes from the group's details, so a rename shows up here.
    groupName: String,
    // Only the admin sees join requests and can edit the group and its members.
    isAdmin: Boolean,
    onBookClick: (isbn: String) -> Unit,
    onChatClick: () -> Unit,
    onInviteClick: () -> Unit,
    onEditClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val group = groupInfo(groupName)

    // What the admin did on this screen, kept as usernames so it survives turning the phone.
    // TODO: send each change to the server instead.
    var acceptedRequests by rememberSaveable { mutableStateOf(listOf<String>()) }
    var rejectedRequests by rememberSaveable { mutableStateOf(listOf<String>()) }
    var newAdmins by rememberSaveable { mutableStateOf(listOf<String>()) }
    var removedMembers by rememberSaveable { mutableStateOf(listOf<String>()) }
    // Who the "Remove from group?" dialog asks about, or null when it's closed.
    var memberToRemove by rememberSaveable { mutableStateOf<String?>(null) }

    val joinRequests = sampleJoinRequests.filter { it.username !in acceptedRequests && it.username !in rejectedRequests }
    // An accepted request makes the person a member.
    val members = (sampleMembers + sampleJoinRequests.filter { it.username in acceptedRequests }
        .map { GroupMember(it.name, it.username, isAdmin = false) })
        .filter { it.username !in removedMembers }
        .map { if (it.username in newAdmins) it.copy(isAdmin = true) else it }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = group.name,
                onBackClick = onBackClick,
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onEditClick) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_group_title))
                        }
                    }
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
            item { GroupHeader(group = group, memberCount = members.size) }
            item { ChatCard(onClick = onChatClick) }

            if (isAdmin && joinRequests.isNotEmpty()) {
                item {
                    SectionHeader(
                        stringResource(R.string.join_requests_count, joinRequests.size),
                        Modifier.padding(top = 4.dp),
                    )
                }
                items(joinRequests, key = { it.username }) { request ->
                    JoinRequestCard(
                        request,
                        onAccept = { acceptedRequests = acceptedRequests + request.username },
                        onReject = { rejectedRequests = rejectedRequests + request.username },
                    )
                }
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
                GroupTab.Members -> items(members, key = { it.username }) { member ->
                    MemberCard(
                        member,
                        // The admin can manage members, but not other admins.
                        canManage = isAdmin && !member.isAdmin,
                        onMakeAdmin = { newAdmins = newAdmins + member.username },
                        onRemove = { memberToRemove = member.username },
                    )
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

    // Asks first, so nobody gets removed by a slip of the finger.
    members.find { it.username == memberToRemove }?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.remove_member_title, member.name, group.name)) },
            text = { Text(stringResource(R.string.remove_member_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removedMembers = removedMembers + member.username
                        memberToRemove = null
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
}

// Group picture, name, "4 members · Private" and the description.
@Composable
private fun GroupHeader(group: GroupInfo, memberCount: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarPlaceholder(size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(
                        R.string.group_members_visibility,
                        pluralStringResource(R.plurals.members, memberCount, memberCount),
                        stringResource(group.visibility.title),
                    ),
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
private fun JoinRequestCard(request: JoinRequest, onAccept: () -> Unit, onReject: () -> Unit) {
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
            OutlinedButton(onClick = onReject, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_reject))
            }
            Button(onClick = onAccept, shape = ButtonShape, modifier = Modifier.weight(1f)) {
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
// With canManage there's also a ⋮ menu: Make admin and Remove from group.
@Composable
private fun MemberCard(member: GroupMember, canManage: Boolean, onMakeAdmin: () -> Unit, onRemove: () -> Unit) {
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
    BookWormsTheme {
        GroupDetailsScreen(
            groupName = "Czytelnia 23",
            isAdmin = true,
            onBookClick = {},
            onChatClick = {},
            onInviteClick = {},
            onEditClick = {},
            onLeaveClick = {},
            onBackClick = {},
        )
    }
}
