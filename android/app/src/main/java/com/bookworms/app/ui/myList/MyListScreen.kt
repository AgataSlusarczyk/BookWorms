package com.bookworms.app.ui.lists

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.*
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.PlaceholderGray
import com.bookworms.app.ui.theme.StatusAmber
import com.bookworms.app.ui.theme.StatusAmberBg
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg

private val CardShape = RoundedCornerShape(12.dp)

// What can happen to a book on the user's list, with the chip colors for each case.
private enum class ListStatus(@StringRes val label: Int, val color: Color, val background: Color) {
    Available(R.string.status_available, StatusGreen, StatusGreenBg),
    Borrowed(R.string.status_borrowed, BrandBlue, StatusBlueBg),
    Reserved(R.string.status_reserved, StatusAmber, StatusAmberBg),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListsScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.tab_favorites, R.string.tab_want_to_read, R.string.tab_read)

    Scaffold(
        
        modifier = Modifier.mistedBackground(), // Zamiast pustego Modifier
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.my_lists_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
                HorizontalDivider(color = CardBorder)
            }
        },
        bottomBar = { AppBottomBar("lists") }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                SegmentedTabs(
                    tabs = tabs.map { stringResource(it) },
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it },
                )
            }
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

            item { HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp)) }

            item { ReadingStatsCard() }
            item { MostBorrowedCard() }
        }
    }
}

// White card with a thin light border – the base for every card on this screen.
@Composable
private fun ListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        shape = CardShape,
        border = BorderStroke(1.dp, CardBorder),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun ListBookCard(title: String, author: String, status: ListStatus, extraBadge: String? = null) {
    ListCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .background(PlaceholderGray, RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
            // weight(1f) takes the free space, so the heart is pushed to the right edge.
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusTag(
                        text = stringResource(status.label),
                        dotColor = status.color,
                        backgroundColor = status.background,
                        textColor = status.color,
                    )
                    if (extraBadge != null) {
                        Text(
                            text = extraBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .background(PlaceholderGray, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_favorite_border),
                contentDescription = stringResource(R.string.content_desc_favorite),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReadingStatsCard() {
    // Share of the tallest bar, from 0 to 1. The last month is highlighted.
    val heights = listOf(0.3f, 0.45f, 0.22f, 0.65f, 0.4f, 1f)
    val labels = listOf("VI", "VII", "VIII", "IX", "X", "XI")

    ListCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.stats_books_read_in_year, "2026"),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("12", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            // Every bar gets an equal share of the width (weight 1), with 8dp gaps between them.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                heights.forEachIndexed { index, height ->
                    val isLast = index == heights.lastIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(height)
                            .background(
                                if (isLast) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            )
                    )
                }
            }

            HorizontalDivider(color = CardBorder)
            Spacer(Modifier.height(8.dp))

            // Same weights as the bars, so every label sits right under its bar.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun MostBorrowedCard() {
    val items = listOf("1. Problem trzech ciał" to 9, "2. Solaris" to 7, "3. Lalka" to 5)

    ListCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_most_borrowed), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            items.forEach { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(
                        "$count×",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyListsScreenPreview() {
    BookWormsTheme {
        MyListsScreen()
    }
}
