package com.bookworms.app.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.Friend
import com.bookworms.app.data.FriendRequest
import com.bookworms.app.data.FriendsOverview
import com.bookworms.app.data.GroupInvitation
import com.bookworms.app.data.network.GroupSummaryDto
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.mistedBackground

@Composable
internal fun FriendsScreen(
    overview: FriendsOverview,
    query: String,
    searchResults: List<Friend>,
    busy: Boolean,
    onQueryChange: (String) -> Unit,
    onSendRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onAcceptGroupInvitation: (String) -> Unit,
    onRejectGroupInvitation: (String) -> Unit,
    onFriendClick: (Friend) -> Unit,
    onInviteFriendToGroup: (groupId: String, userId: String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var friendToInvite by remember { mutableStateOf<Friend?>(null) }

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.friends_title),
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (overview.isOffline) {
                item {
                    AppCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.friends_offline),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onRetryClick) {
                                Text(stringResource(R.string.try_again))
                            }
                        }
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_by_username)) },
                        singleLine = true,
                        shape = FieldShape,
                        colors = authFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (query.trim().length >= 2) {
                    item { SectionHeader(stringResource(R.string.search_results)) }
                    if (searchResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_users_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(searchResults, key = { "search_${it.id}" }) { user ->
                            SearchUserCard(
                                user = user,
                                enabled = !busy,
                                onAdd = { onSendRequest(user.id) },
                            )
                        }
                    }
                }

                if (overview.groupInvitations.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.group_invitations_header, overview.groupInvitations.size))
                    }
                    items(overview.groupInvitations, key = { "ginv_${it.id}" }) { invitation ->
                        GroupInvitationCard(
                            invitation = invitation,
                            enabled = !busy,
                            onAccept = { onAcceptGroupInvitation(invitation.id) },
                            onReject = { onRejectGroupInvitation(invitation.id) },
                        )
                    }
                }

                if (overview.incomingRequests.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.friend_requests_header, overview.incomingRequests.size))
                    }
                    items(overview.incomingRequests, key = { "req_${it.id}" }) { request ->
                        FriendRequestCard(
                            request = request,
                            enabled = !busy,
                            onAccept = { onAcceptRequest(request.id) },
                            onReject = { onRejectRequest(request.id) },
                        )
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.your_friends_header, overview.friends.size))
            }

            if (overview.friends.isEmpty()) {
                item {
                    Text(
                        text = stringResource(if (overview.isOffline) R.string.friends_empty_offline else R.string.friends_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(overview.friends, key = { it.id }) { friend ->
                    FriendCard(
                        friend = friend,
                        offline = overview.isOffline,
                        enabled = !busy,
                        onClick = { onFriendClick(friend) },
                        onInviteToGroup = { friendToInvite = friend },
                        onRemove = { onRemoveFriend(friend.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    friendToInvite?.let { targetFriend ->
        InviteToGroupDialog(
            friendName = targetFriend.name,
            groups = overview.myGroups,
            onSelectGroup = { groupId ->
                friendToInvite = null
                onInviteFriendToGroup(groupId, targetFriend.id)
            },
            onDismiss = { friendToInvite = null },
        )
    }
}

@Composable
internal fun InviteToGroupDialog(
    friendName: String,
    groups: List<GroupSummaryDto>,
    onSelectGroup: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invite_friend_title, friendName)) },
        text = {
            if (groups.isEmpty()) {
                Text(stringResource(R.string.no_groups_to_invite))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.forEach { group ->
                        TextButton(
                            onClick = { onSelectGroup(group.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = group.name,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SearchUserCard(
    user: Friend,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ProfilePhoto(photo = user.photoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.username_handle, user.username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onAdd, enabled = enabled, shape = FieldShape) {
                Text(stringResource(R.string.add_friend))
            }
        }
    }
}

@Composable
private fun GroupInvitationCard(
    invitation: GroupInvitation,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Column {
                Text(
                    text = invitation.groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.invited_by, invitation.inviterName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = enabled,
                    shape = FieldShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_join))
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = enabled,
                    shape = FieldShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_decline))
                }
            }
        }
    }
}

@Composable
private fun FriendRequestCard(
    request: FriendRequest,
    enabled: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(photo = request.requesterPhotoUrl, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = request.requesterName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.username_handle, request.requesterUsername),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = enabled,
                    shape = FieldShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_accept))
                }
                OutlinedButton(
                    onClick = onReject,
                    enabled = enabled,
                    shape = FieldShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_reject))
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    friend: Friend,
    offline: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onInviteToGroup: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!offline) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            ProfilePhoto(photo = friend.photoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.username_handle, friend.username),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!offline) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = enabled) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.invite_title)) },
                            onClick = {
                                menuExpanded = false
                                onInviteToGroup()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_friend), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}
