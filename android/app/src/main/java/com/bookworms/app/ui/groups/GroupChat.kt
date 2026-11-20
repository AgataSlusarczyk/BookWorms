package com.bookworms.app.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.data.auth.Session
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TodayTime = DateTimeFormatter.ofPattern("HH:mm")
private val EarlierTime = DateTimeFormatter.ofPattern("dd.MM HH:mm")

// How close to the oldest message on screen the next older page starts loading.
private const val LoadOlderAhead = 5

// The Chat tab of Group details, with its live connection: open while the tab is on screen and the app
// is in front, closed when the user looks elsewhere – so a phone in a pocket keeps no connection open.
@Composable
internal fun GroupChat(viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val loadingOlder by viewModel.loadingOlder.collectAsState()
    val sending by viewModel.sending.collectAsState()
    val user by Session.user.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.stayConnected() }
    }

    GroupChatContent(
        messages = messages,
        myId = user?.id,
        connected = connected,
        loadingOlder = loadingOlder,
        draft = viewModel.draft,
        sending = sending,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::send,
        onLoadOlder = viewModel::loadOlder,
        onRetryClick = viewModel::refresh,
        modifier = modifier,
    )
}

// Messages from the bottom up (the newest at the bottom, like in any messenger) and the text field under them.
@Composable
private fun GroupChatContent(
    messages: LoadState<List<ChatMessage>>,
    // null = not known, so nothing is drawn as the user's own.
    myId: String?,
    connected: Boolean,
    loadingOlder: Boolean,
    draft: String,
    sending: Boolean,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onLoadOlder: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (messages) {
                LoadState.Loading -> LoadingIndicator()
                LoadState.Failed -> LoadFailedMessage(stringResource(R.string.load_chat_failed), onRetryClick)
                is LoadState.Ready -> if (messages.data.isEmpty()) {
                    Text(
                        stringResource(R.string.chat_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                } else {
                    MessageList(messages.data, myId, loadingOlder, onLoadOlder)
                }
            }
            if (!connected && messages is LoadState.Ready) {
                Text(
                    stringResource(R.string.chat_connecting),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        MessageField(draft, sending, onDraftChange, onSendClick)
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, myId: String?, loadingOlder: Boolean, onLoadOlder: () -> Unit) {
    val listState = rememberLazyListState()

    // A new message at the bottom: scrolled to when the user is already at the bottom, or sent it themselves.
    // Someone reading older messages stays where they are.
    val newest = messages.first()
    LaunchedEffect(newest.id) {
        if (listState.firstVisibleItemIndex <= 1 || newest.senderId == myId) listState.animateScrollToItem(0)
    }

    // Near the oldest message on screen – time for the page before it.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LoadOlderAhead
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadOlder() }
    }

    LazyColumn(
        state = listState,
        // Index 0 (the newest) at the bottom.
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
            val mine = message.senderId == myId
            // The name only over the first of someone's messages in a row – the one right above is older (index + 1).
            val showName = !mine && messages.getOrNull(index + 1)?.senderId != message.senderId
            MessageBubble(message, mine, showName)
        }
        if (loadingOlder) {
            item(key = "loading-older") { LoadingIndicator() }
        }
    }
}

// The user's own messages on the right in blue, everyone else's on the left in white, with the name over them.
@Composable
private fun MessageBubble(message: ChatMessage, mine: Boolean, showName: Boolean) {
    Column(
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (showName) 8.dp else 0.dp),
    ) {
        if (showName) {
            Text(
                message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp),
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (mine) BrandBlue else MaterialTheme.colorScheme.surface,
                    // The corner nearest the sender's side is sharper, like a speech bubble's tail.
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (mine) 16.dp else 4.dp,
                        bottomEnd = if (mine) 4.dp else 16.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                sentTime(message.sentAt),
                style = MaterialTheme.typography.labelSmall,
                color = if (mine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun MessageField(draft: String, sending: Boolean, onDraftChange: (String) -> Unit, onSendClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text(stringResource(R.string.chat_message_hint)) },
            maxLines = 4,
            shape = FieldShape,
            colors = authFieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onSendClick,
            enabled = draft.isNotBlank() && !sending,
            colors = IconButtonDefaults.iconButtonColors(contentColor = BrandBlue),
        ) {
            Icon(painterResource(R.drawable.ic_send), contentDescription = stringResource(R.string.chat_send))
        }
    }
}

// "14:05" for today, "18.11 14:05" for earlier days.
private fun sentTime(sentAt: Instant): String {
    val time = sentAt.atZone(ZoneId.systemDefault())
    return time.format(if (time.toLocalDate() == LocalDate.now()) TodayTime else EarlierTime)
}

@Preview(device = "id:pixel_8", showBackground = true)
@Composable
private fun GroupChatPreview() {
    val now = Instant.now()
    BookWormsTheme {
        GroupChatContent(
            messages = LoadState.Ready(
                listOf(
                    ChatMessage("4", "me", "Anna", "Great, I'll bring it on Thursday.", now),
                    ChatMessage("3", "o", "Ola Nowak", "Me! Finishing it tonight 📚", now.minusSeconds(60)),
                    ChatMessage("2", "o", "Ola Nowak", "Hi all", now.minusSeconds(90)),
                    ChatMessage("1", "me", "Anna", "Who's got Solaris right now?", now.minusSeconds(3600)),
                ),
            ),
            myId = "me",
            connected = true,
            loadingOlder = false,
            draft = "",
            sending = false,
            onDraftChange = {},
            onSendClick = {},
            onLoadOlder = {},
            onRetryClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
