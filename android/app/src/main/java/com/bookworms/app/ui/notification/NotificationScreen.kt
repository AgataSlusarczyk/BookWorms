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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { }) { 
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back), 
                                contentDescription = stringResource(R.string.content_desc_back)
                            ) 
                        }
                    },
                    actions = {
                        TextButton(onClick = { }) { Text(stringResource(R.string.action_mark_read)) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { 
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp)) {
                        Text(stringResource(R.string.date_today), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) 
                    }
                }
                
                item {
                    NotificationActionCard(
                        title = "Krzysiek P. prosi o Solaris", 
                        time = "2 h temu", 
                        isUnread = true
                    )
                }
                item {
                    NotificationSimpleCard(
                        title = "Pachnidło – 4 dni po terminie zwrotu", 
                        time = "dziś, 8:00 • automatyczne przypomnienie",
                        iconResId = R.drawable.ic_schedule, 
                        iconColor = Color(0xFFB8860B)
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { 
                    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp)) {
                        Text(stringResource(R.string.date_yesterday), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) 
                    }
                }
                
                item {
                    NotificationSimpleCard(
                        title = "Problem trzech ciał z Twojej listy jest znów dostępna", 
                        time = "wczoraj, 19:12",
                        iconResId = R.drawable.ic_book, 
                        iconColor = Color.Blue
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationActionCard(title: String, time: String, isUnread: Boolean) {
    OutlinedCard(
        border = BorderStroke(if (isUnread) 2.dp else 1.dp, if (isUnread) Color.Blue else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (isUnread) {
                    Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).background(Color.Blue, CircleShape))
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