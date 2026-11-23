package com.bookworms.app.ui.friends

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.Friend
import com.bookworms.app.data.FriendBook
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.network.GroupSummaryDto
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookCover
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.SegmentedTabs
import com.bookworms.app.ui.components.mistedBackground

@Composable
internal fun FriendProfileScreen(
    friend: Friend,
    myGroups: List<GroupSummaryDto>,
    booksState: LoadState<List<FriendBook>>,
    busy: Boolean,
    onRetryBooks: () -> Unit,
    onBookClick: (String) -> Unit,
    onInviteToGroup: (String) -> Unit,
    onRemoveFriend: () -> Unit,
    onBackClick: () -> Unit,
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_friend_books), stringResource(R.string.tab_friend_stats))

    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = friend.name,
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
            item {
                Spacer(Modifier.height(4.dp))
                AppCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        ProfilePhoto(photo = friend.photoUrl, size = 72.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = friend.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.username_handle, friend.username),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                        ) {
                            Button(
                                onClick = { showInviteDialog = true },
                                enabled = !busy,
                                shape = FieldShape,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.invite_title))
                            }
                            OutlinedButton(
                                onClick = onRemoveFriend,
                                enabled = !busy,
                                shape = FieldShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.action_remove))
                            }
                        }
                    }
                }
            }

            item {
                SegmentedTabs(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            if (selectedTab == 0) {
                when (booksState) {
                    LoadState.Loading -> {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    LoadState.Failed -> {
                        item {
                            AppCard {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.load_friend_books_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = onRetryBooks) {
                                        Text(stringResource(R.string.try_again))
                                    }
                                }
                            }
                        }
                    }
                    is LoadState.Ready -> {
                        val books = booksState.data
                        if (books.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.friend_books_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            items(books, key = { it.copyId }) { book ->
                                FriendBookCard(
                                    book = book,
                                    onClick = book.isbn?.let { isbn -> { onBookClick(isbn) } },
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    AppCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.friend_stats_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.friend_stats_later),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showInviteDialog) {
        InviteToGroupDialog(
            friendName = friend.name,
            groups = myGroups,
            onSelectGroup = { groupId ->
                showInviteDialog = false
                onInviteToGroup(groupId)
            },
            onDismiss = { showInviteDialog = false },
        )
    }
}

// Cover, title, author and year – like a book on Group details. A tap opens Book details (books with an ISBN).
@Composable
private fun FriendBookCard(book: FriendBook, onClick: (() -> Unit)?) {
    AppCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(12.dp),
        ) {
            BookCover(book.coverUrl)
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    book.authors?.takeIf { it.isNotBlank() },
                    book.publishedYear?.toString(),
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
