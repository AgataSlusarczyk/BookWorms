// AppSharedComponents.kt
package com.bookworms.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppBottomBar(currentRoute: String = "Wypożyczenia") {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf("Biblioteka" to Icons.Default.Book, "Wypożyczenia" to Icons.Default.SwapHoriz, "Moje listy" to Icons.Default.FormatListBulleted, "Profil" to Icons.Default.Person)
        items.forEach { (title, icon) ->
            NavigationBarItem(
                selected = title == currentRoute,
                onClick = { /* TODO: Nawigacja */ },
                icon = { Icon(icon, contentDescription = title) },
                label = { Text(title) }
            )
        }
    }
}

@Composable
fun StatusTag(text: String, dotColor: Color, backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp, 80.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
    )
}