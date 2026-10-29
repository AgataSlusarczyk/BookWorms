package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.BackTitle
import com.bookworms.app.ui.components.mistedBackground

@Composable
fun MyBooksScreen(
    onBackClick: () -> Unit,
    onAddBookClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackTitle(title = stringResource(R.string.my_books_title), onBackClick = onBackClick)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBookClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add), 
                    contentDescription = stringResource(R.string.content_desc_add) // Dodaj do strings: Add
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Przykładowe własne książki
            items(2) {
                LibraryBookItem(title = "Wiedźmin", author = "Andrzej Sapkowski", year = "1994")
            }
        }
    }
}