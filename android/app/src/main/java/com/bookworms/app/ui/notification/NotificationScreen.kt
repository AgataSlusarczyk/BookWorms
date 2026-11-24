package com.bookworms.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.LoadFailedMessage
import com.bookworms.app.ui.components.LoadingIndicator
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.loans.BookRequest
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.CardBorder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsScreen(
    state: LoadState<NotificationsData>,
    // Answers given on this screen, by request id: true = accepted.
    answers: Map<String, Boolean>,
    refreshing: Boolean,
    saving: Boolean,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onAccept: (NotificationItem) -> Unit,
    onReject: (NotificationItem) -> Unit,
    // A tap on a card: marks it read and opens what it's about, if there's anything to see (see AppNavigation).
    onNotificationClick: (NotificationItem) -> Unit,
    onBackClick: () -> Unit,
) {
    val hasUnread = state is LoadState.Ready && state.data.items.any { !it.read }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notifications_title),
                onBackClick = onBackClick,
                actions = {
                    // Grayed out when there's nothing new.
                    TextButton(onClick = onMarkAllRead, enabled = hasUnread) {
                        Text(stringResource(R.string.action_mark_read))
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            LoadState.Loading -> LoadingIndicator(Modifier.padding(padding))
            LoadState.Failed -> LoadFailedMessage(
                message = stringResource(R.string.load_notifications_failed),
                onRetryClick = onRefresh,
                modifier = Modifier.padding(padding),
            )
            is LoadState.Ready -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                NotificationList(
                    data = state.data,
                    answers = answers,
                    saving = saving,
                    onAccept = onAccept,
                    onReject = onReject,
                    onNotificationClick = onNotificationClick,
                )
            }
        }
    }
}

// The notifications in three groups: Today, Yesterday and Earlier (empty groups are left out).
@Composable
private fun NotificationList(
    data: NotificationsData,
    answers: Map<String, Boolean>,
    saving: Boolean,
    onAccept: (NotificationItem) -> Unit,
    onReject: (NotificationItem) -> Unit,
    onNotificationClick: (NotificationItem) -> Unit,
) {
    val today = LocalDate.now()
    val groups = data.items.groupBy { item ->
        when (item.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()) {
            today -> R.string.date_today
            today.minusDays(1) -> R.string.date_yesterday
            else -> R.string.date_earlier
        }
    }

    // A LazyColumn even when empty, so pull to refresh works there too.
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (data.items.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.notifications_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        }
        for (header in listOf(R.string.date_today, R.string.date_yesterday, R.string.date_earlier)) {
            val items = groups[header] ?: continue
            item(key = "header_$header") { SectionHeader(stringResource(header)) }
            items(items, key = { it.id }) { item ->
                NotificationCard(
                    item = item,
                    // Buttons only while the request still waits for an answer.
                    openRequest = data.openRequests[item.referenceId].takeIf { item.type == NotificationType.BorrowRequested },
                    answer = answers[item.referenceId].takeIf { item.type == NotificationType.BorrowRequested },
                    saving = saving,
                    onAccept = { onAccept(item) },
                    onReject = { onReject(item) },
                    onClick = { onNotificationClick(item) },
                )
            }
        }
    }
}

// One notification: the other person's photo, what happened and when. A blue dot and border = not seen yet.
// A borrow request still waiting for an answer gets Accept and Reject; once answered here,
// the buttons turn into a short line saying what you chose.
@Composable
private fun NotificationCard(
    item: NotificationItem,
    openRequest: BookRequest?,
    answer: Boolean?,
    saving: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit,
) {
    AppCard(borderColor = if (item.read) CardBorder else MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhoto(photo = item.personPhotoUrl, size = 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notificationText(item),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (item.read) FontWeight.Normal else FontWeight.Medium,
                    )
                    Text(
                        text = notificationTime(item.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!item.read) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
            if (answer != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(if (answer) R.string.you_accepted_request else R.string.you_rejected_request),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (openRequest != null) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onReject, enabled = !saving, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_reject))
                    }
                    // Grayed out when the book can't be lent now (e.g. it's lent to someone else) – as on Loans.
                    Button(
                        onClick = onAccept,
                        enabled = !saving && openRequest.canAccept,
                        shape = ButtonShape,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.action_accept))
                    }
                }
            }
        }
    }
}

// "Anna asks to borrow Bieguni", "Anna agreed to lend you Bieguni" and so on.
@Composable
private fun notificationText(item: NotificationItem): String {
    val text = when (item.type) {
        NotificationType.BorrowRequested -> R.string.asks_to_borrow_book
        NotificationType.RequestAccepted -> R.string.notification_request_accepted
        NotificationType.RequestRejected -> R.string.notification_request_rejected
        NotificationType.RequestCanceled -> R.string.notification_request_canceled
        NotificationType.ReturnConfirmed -> R.string.notification_return_confirmed
        NotificationType.ReturnDue -> R.string.notification_return_due
        NotificationType.ReturnOverdue -> R.string.notification_return_overdue
        NotificationType.CopyAvailable -> R.string.notification_copy_available
    }
    return stringResource(text, item.personName, item.bookTitle)
}

// Today: "Just now" or "5 h ago". Before that: the day and hour, e.g. "21 Nov, 18:40".
@Composable
private fun notificationTime(moment: Instant): String {
    val local = moment.atZone(ZoneId.systemDefault())
    if (local.toLocalDate() != LocalDate.now()) return local.format(DateTimeFormatter.ofPattern("d MMM, HH:mm"))
    val hours = Duration.between(moment, Instant.now()).toHours()
    return if (hours < 1) stringResource(R.string.just_now) else stringResource(R.string.x_hours_ago, hours.toString())
}

@Preview(device = "id:pixel_8")
@Composable
private fun NotificationsScreenPreview() {
    val now = Instant.now()
    val request = BookRequest("r1", "Bieguni", null, null, "Anna K.", now.minusSeconds(2 * 3600), canAccept = true)
    BookWormsTheme {
        NotificationsScreen(
            state = LoadState.Ready(
                NotificationsData(
                    items = listOf(
                        NotificationItem("1", NotificationType.BorrowRequested, "r1", false, now.minusSeconds(2 * 3600), "Bieguni", "9788308044435", "Anna K.", null),
                        NotificationItem("2", NotificationType.RequestAccepted, "r2", true, now.minusSeconds(26 * 3600), "Pachnidło", null, "Marek", null),
                        NotificationItem("3", NotificationType.CopyAvailable, "c1", true, now.minusSeconds(5 * 86400), "Problem trzech ciał", null, "Zosia", null),
                    ),
                    openRequests = mapOf("r1" to request),
                ),
            ),
            answers = emptyMap(),
            refreshing = false,
            saving = false,
            onRefresh = {},
            onMarkAllRead = {},
            onAccept = {},
            onReject = {},
            onNotificationClick = {},
            onBackClick = {},
        )
    }
}
