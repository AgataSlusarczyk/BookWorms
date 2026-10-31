package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

// The books on one of the user's own lists, opened from "Custom lists" in My lists.
@Composable
fun MyListDetailsScreen(listName: String, onBackClick: () -> Unit) {
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = listName, onBackClick = onBackClick) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // TODO: the books on this list from the server.
            item { ListBookCard("Solaris", "Stanisław Lem", ListStatus.Available) }
            item {
                ListBookCard(
                    title = "Pachnidło",
                    author = "Patrick Süskind",
                    status = ListStatus.Borrowed,
                    extraBadge = stringResource(R.string.badge_notify_me),
                )
            }
            item { ListBookCard("Czarna Ziemia", "Timothy Snyder", ListStatus.Reserved) }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyListDetailsScreenPreview() {
    BookWormsTheme {
        MyListDetailsScreen(listName = "Sci-fi", onBackClick = {})
    }
}
