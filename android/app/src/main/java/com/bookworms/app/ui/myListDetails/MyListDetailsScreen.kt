package com.bookworms.app.ui.lists

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground

@Composable
fun MyListDetailsScreen(listName: String, onBackClick: () -> Unit) {
    val decodedName = Uri.decode(listName)
    
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = decodedName, onBackClick = onBackClick) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item {
                ListBookCard(
                    title = "Solaris", 
                    author = "Stanisław Lem", 
                    status = stringResource(R.string.status_available), 
                    dotColor = Color(0xFF2E7D32) // Zielony
                )
            }
            item {
                ListBookCard(
                    title = "Pachnidło", 
                    author = "Patrick Süskind", 
                    status = stringResource(R.string.status_borrowed), 
                    dotColor = Color.Blue, // Niebieski
                    extraBadge = stringResource(R.string.badge_notify_me)
                )
            }
            item {
                ListBookCard(
                    title = "Czarna Ziemia", 
                    author = "Timothy Snyder", 
                    status = stringResource(R.string.status_reserved), 
                    dotColor = Color(0xFFE6B800) // Bursztynowy
                )
            }
        }
    }
}