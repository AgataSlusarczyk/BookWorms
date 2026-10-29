package com.bookworms.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ButtonShape
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.StatusAmber
import com.bookworms.app.ui.theme.StatusAmberBg
import com.bookworms.app.ui.theme.StatusBlueBg

@Composable
fun NotificationsScreen(onBackClick: () -> Unit = {}) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notifications_title),
                onBackClick = onBackClick,
                actions = {
                    TextButton(onClick = { }) { Text(stringResource(R.string.action_mark_read)) }
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
            item { SectionHeader(stringResource(R.string.date_today)) }
            item {
                NotificationActionCard(
                    title = stringResource(R.string.asks_to_borrow_book, "Krzysiek P.", "Solaris"),
                    time = stringResource(R.string.x_hours_ago, "2"),
                    isUnread = true,
                )
            }
            item {
                NotificationSimpleCard(
                    title = stringResource(R.string.book_days_overdue, "Pachnidło", "4"),
                    time = stringResource(R.string.automatic_reminder, stringResource(R.string.today_at_time, "8:00")),
                    iconResId = R.drawable.ic_schedule,
                    iconColor = StatusAmber,
                    iconBackground = StatusAmberBg,
                )
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader(stringResource(R.string.date_yesterday)) }
            item {
                NotificationSimpleCard(
                    title = stringResource(R.string.book_available_again, "Problem trzech ciał"),
                    time = stringResource(R.string.yesterday_at_time, "19:12"),
                    iconResId = R.drawable.ic_book,
                    iconColor = BrandBlue,
                    iconBackground = StatusBlueBg,
                )
            }
        }
    }
}

// The shared white card; an unread notification gets a blue border instead of the gray one.
@Composable
private fun NotificationCard(isUnread: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    AppCard(borderColor = if (isUnread) MaterialTheme.colorScheme.primary else CardBorder, content = content)
}

@Composable
fun NotificationActionCard(title: String, time: String, isUnread: Boolean) {
    NotificationCard(isUnread = isUnread) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Blue dot = you haven't seen this one yet.
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_reject))
                }
                Button(onClick = { }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_accept))
                }
            }
        }
    }
}

@Composable
fun NotificationSimpleCard(title: String, time: String, iconResId: Int, iconColor: Color, iconBackground: Color) {
    NotificationCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            // Icon in a soft colored circle – the same colors as the status chips.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackground, CircleShape),
            ) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun NotificationsScreenPreview() {
    BookWormsTheme {
        NotificationsScreen()
    }
}
