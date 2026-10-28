package com.bookworms.app.ui.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.components.BackTitle 
import com.bookworms.app.ui.theme.BookWormsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBackClick: () -> Unit = {}) {
    Scaffold(
        modifier = Modifier.mistedBackground(), // Zamiast pustego Modifier
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    BackTitle(title = stringResource(R.string.notifications_title), onBackClick = onBackClick) 
                },
                actions = {
                    TextButton(onClick = { }) { Text(stringResource(R.string.action_mark_read)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.date_today), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            
            item {
                NotificationActionCard(
                    title = stringResource(R.string.asks_to_borrow_book, "Krzysiek P.", "Solaris"), 
                    time = stringResource(R.string.x_hours_ago, "2"), 
                    isUnread = true
                )
            }
            item {
                NotificationSimpleCard(
                    title = stringResource(R.string.book_days_overdue, "Pachnidło", "4"), 
                    time = stringResource(R.string.automatic_reminder, stringResource(R.string.today_at_time, "8:00")),
                    iconResId = R.drawable.ic_schedule, 
                    iconColor = MaterialTheme.colorScheme.tertiary
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text(stringResource(R.string.date_yesterday), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            
            item {
                NotificationSimpleCard(
                    title = stringResource(R.string.book_available_again, "Problem trzech ciał"), 
                    time = stringResource(R.string.yesterday_at_time, "19:12"),
                    iconResId = R.drawable.ic_book, 
                    iconColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun NotificationActionCard(title: String, time: String, isUnread: Boolean) {
    val borderColor = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isUnread) 2.dp else 1.dp

    OutlinedCard(
        border = BorderStroke(borderWidth, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (isUnread) {
                    Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = if (isUnread) 20.dp else 0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.action_reject)) }
                Button(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.action_accept)) }
            }
        }
    }
}

@Composable
fun NotificationSimpleCard(title: String, time: String, iconResId: Int, iconColor: Color) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(painter = painterResource(id = iconResId), contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}

@Preview
@Composable
private fun NotificationsScreenPreview() {
    BookWormsTheme {
        NotificationsScreen()
    }
}