package com.bookworms.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_login),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

@Composable
fun AppBottomBar(currentRoute: String = "loans") {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = listOf(
            Triple("library", R.string.nav_library, R.drawable.ic_library),
            Triple("loans", R.string.nav_loans, R.drawable.ic_loans),
            Triple("lists", R.string.nav_my_lists, R.drawable.ic_lists),
            Triple("profile", R.string.nav_profile, R.drawable.ic_profile)
        )
        
        items.forEach { (route, titleResId, iconResId) ->
            NavigationBarItem(
                selected = route == currentRoute,
                onClick = { /* TODO: Nawigacja */ },
                icon = { 
                    Icon(
                        painter = painterResource(id = iconResId), 
                        contentDescription = stringResource(id = titleResId)
                    ) 
                },
                label = { Text(stringResource(id = titleResId)) }
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
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
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