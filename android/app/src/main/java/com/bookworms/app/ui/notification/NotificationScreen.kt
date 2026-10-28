// NotificationsScreen.kt
package com.bookworms.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Powiadomienia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { }) { Icon(Icons.Default.ArrowBack, "Wstecz") }
                },
                actions = {
                    TextButton(onClick = { }) { Text("Oznacz") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Dzisiaj", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            
            item {
                NotificationActionCard(
                    title = "Krzysiek P. prosi o Solaris", time = "2 h temu", isUnread = true
                )
            }
            item {
                NotificationSimpleCard(
                    title = "Pachnidło – 4 dni po terminie zwrotu", time = "dziś, 8:00 • automatyczne przypomnienie",
                    icon = Icons.Default.Schedule, iconColor = Color(0xFFB8860B)
                )
            }
            item {
                NotificationSimpleCard(
                    title = "Anna K. potwierdziła przekazanie Lalki", time = "dziś, 7:41",
                    icon = Icons.Default.Check, iconColor = Color(0xFF2E7D32)
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
            item { Text("Wczoraj", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            
            item {
                NotificationSimpleCard(
                    title = "Problem trzech ciał z Twojej listy jest znów dostępna", time = "wczoraj, 19:12",
                    icon = Icons.Default.Book, iconColor = Color.Blue
                )
            }
            item {
                NotificationSimpleCard(
                    title = "Twoja prośba o Czarną Ziemię została odrzucona...", time = "wczoraj, 15:33",
                    icon = Icons.Default.Close, iconColor = Color.Gray
                )
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
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Odrzuć") }
                Button(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Akceptuj") }
            }
        }
    }
}

@Composable
fun NotificationSimpleCard(title: String, time: String, icon: ImageVector, iconColor: Color) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
    }
}