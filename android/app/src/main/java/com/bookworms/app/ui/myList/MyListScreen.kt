package com.bookworms.app.ui.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppBottomBar
import com.bookworms.app.ui.components.mistedBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListsScreen(onNavigate: (String) -> Unit, onListClick: (String) -> Unit) {
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.my_lists_title), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = { AppBottomBar("lists", onNavigate) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListEntryCard(stringResource(R.string.tab_favorites), onClick = { onListClick("Favorites") })
                    ListEntryCard(stringResource(R.string.tab_want_to_read), onClick = { onListClick("Want to read") })
                    ListEntryCard(stringResource(R.string.tab_read), onClick = { onListClick("Read") })
                }
            }

            item {
                Text(stringResource(R.string.custom_lists), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        OutlinedCard(onClick = { }, modifier = Modifier.size(120.dp, 80.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(painterResource(R.drawable.ic_add), null)
                                Text(stringResource(R.string.create_list), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    items(3) { index ->
                        OutlinedCard(onClick = { onListClick("Custom List $index") }, modifier = Modifier.size(120.dp, 80.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text("Custom List $index", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { ReadingStatsCard() }
            item { MostBorrowedCard() }
        }
    }
}

@Composable
fun ListEntryCard(title: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}