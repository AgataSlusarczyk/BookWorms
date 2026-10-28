package com.bookworms.app.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.bookworms.app.ui.components.*
import com.bookworms.app.ui.theme.BookWormsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListsScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.tab_favorites, R.string.tab_want_to_read, R.string.tab_read)

    Scaffold(
        modifier = Modifier, 
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.my_lists_title), fontWeight = FontWeight.Bold) }
            ) 
        },
        bottomBar = { AppBottomBar("lists") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, titleResId ->
                    Tab(
                        selected = selectedTabIndex == index, 
                        onClick = { selectedTabIndex = index }, 
                        text = { Text(stringResource(titleResId)) }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ListBookCard(
                        title = "Solaris", 
                        author = "Stanisław Lem", 
                        status = stringResource(R.string.status_available), 
                        dotColor = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    ListBookCard(
                        title = "Pachnidło", 
                        author = "Patrick Süskind", 
                        status = stringResource(R.string.status_borrowed), 
                        dotColor = MaterialTheme.colorScheme.secondary, 
                        extraBadge = stringResource(R.string.badge_notify_me)
                    )
                }
                item {
                    ListBookCard(
                        title = "Czarna Ziemia", 
                        author = "Timothy Snyder", 
                        status = stringResource(R.string.status_reserved), 
                        dotColor = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ReadingStatsCard() }
                item { MostBorrowedCard() }
            }
        }
    }
}

@Composable
fun ListBookCard(title: String, author: String, status: String, dotColor: Color, extraBadge: String? = null) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = Alignment.Top
        ) {
            Row {
                BookCoverPlaceholder()
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusTag(status, dotColor)
                        if (extraBadge != null) {
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(extraBadge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_favorite_border), 
                contentDescription = stringResource(R.string.content_desc_favorite), 
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ReadingStatsCard() {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.stats_books_read_in_year, "2026"), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("12", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp), 
                horizontalArrangement = Arrangement.SpaceEvenly, 
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.3f, 0.5f, 0.2f, 0.6f, 0.4f, 0.9f)
                val labels = listOf("VI", "VII", "VIII", "IX", "X", "XI")
                heights.zip(labels).forEach { (h, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .fillMaxHeight(h)
                                .background(
                                    color = if (h == 0.9f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, 
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun MostBorrowedCard() {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), 
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_most_borrowed), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            val items = listOf("1. Problem trzech ciał" to "9x", "2. Solaris" to "7x", "3. Lalka" to "5x")
            items.forEach { (name, count) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontSize = 14.sp)
                    Text(count, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview
@Composable
private fun MyListsScreenPreview() {
    BookWormsTheme {
        MyListsScreen()
    }
}