package com.bookworms.app.ui.lists

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
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

// What can happen to a book on the user's list, with the chip colors for each case.
internal enum class ListStatus(@StringRes val label: Int, val color: Color, val background: Color) {
    Available(R.string.status_available, StatusGreen, StatusGreenBg),
    Borrowed(R.string.status_borrowed, BrandBlue, StatusBlueBg),
    Reserved(R.string.status_reserved, StatusAmber, StatusAmberBg),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyListsScreen(
    onNavigate: (String) -> Unit = {},
    onListClick: (String) -> Unit = {},
    onCreateListClick: () -> Unit = {},
    onBookClick: (isbn: String) -> Unit = {},
    // Opens the whole shelf on its own screen.
    onSeeAllClick: (Shelf) -> Unit = {},
    // The books on each shelf, from the server.
    shelves: LoadState<Map<Shelf, List<ShelfBook>>> = LoadState.Ready(emptyMap()),
    onRetryClick: () -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val shelf = Shelf.entries[selectedTabIndex]
    val allBooks = (shelves as? LoadState.Ready)?.data?.get(shelf).orEmpty()
    // Only the newest few here, so "Your lists" and the stats stay close. The rest is behind "See all".
    val previewBooks = ShelfSort.RecentlyAdded.apply(allBooks).take(ShelfPreviewCount)

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.my_lists_title)) },
        bottomBar = { AppBottomBar(currentRoute = "lists", onNavigate = onNavigate) }
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
                    tabs = Shelf.entries.map { stringResource(it.label) },
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it },
                )
            }
            when (shelves) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item {
                    LoadFailedMessage(stringResource(R.string.load_shelves_failed), onRetryClick = onRetryClick)
                }
                is LoadState.Ready -> if (allBooks.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.shelf_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(previewBooks, key = { it.isbn }) { book ->
                ShelfBookCard(book, onClick = { onBookClick(book.isbn) })
            }
            // Only when there's more than the preview shows.
            if (allBooks.size > previewBooks.size) {
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onSeeAllClick(shelf) }) {
                            Text(stringResource(R.string.see_all, allBooks.size))
                            Icon(
                                painterResource(R.drawable.ic_chevron_right),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.custom_lists), Modifier.padding(top = 4.dp)) }
            item { CustomListsRow(onListClick = onListClick, onCreateListClick = onCreateListClick) }

            item { HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp)) }

            // Only once there's something to show – see readingYears and mostBorrowed.
            if (readingYears.isNotEmpty()) item { ReadingStatsCard() }
            if (mostBorrowed.isNotEmpty()) item { MostBorrowedCard(onBookClick = onBookClick) }
        }
    }
}

private const val ShelfPreviewCount = 3

// The cards on this screen use the shared white card with a thin border.
@Composable
private fun ListCard(content: @Composable ColumnScope.() -> Unit) = AppCard(content = content)

// TODO: the user's own lists from the server. Empty until then – only "Create list" shows.
internal val customLists: List<String> = emptyList()

// Lists the user made themselves, side by side, with a "Create list" tile first.
@Composable
private fun CustomListsRow(onListClick: (String) -> Unit, onCreateListClick: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AppCard(onClick = onCreateListClick, modifier = Modifier.width(CustomListTileWidth)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CustomListTileHeight),
                ) {
                    Icon(painterResource(R.drawable.ic_add), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.create_list), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        items(customLists) { name ->
            AppCard(onClick = { onListClick(name) }, modifier = Modifier.width(CustomListTileWidth)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CustomListTileHeight)
                        .padding(8.dp),
                ) {
                    Text(name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

private val CustomListTileWidth = 120.dp
private val CustomListTileHeight = 80.dp

@Composable
internal fun ListBookCard(
    title: String,
    author: String,
    // Whether the book can be borrowed now. null = not shown (the shelves don't check that yet).
    status: ListStatus?,
    onClick: () -> Unit,
    // A full heart for books on Favorites, an empty one for the rest.
    isFavorite: Boolean = false,
    extraBadge: String? = null,
    // When the book was put on the list; shown after the author, e.g. "Stanisław Lem · added 5 days ago".
    addedDaysAgo: Int? = null,
    // Full address of the cover. null = the gray box.
    coverUrl: String? = null,
) {
    AppCard(onClick = onClick) {
        BookRow(
            title = title,
            subtitle = if (addedDaysAgo == null) author else
                "$author · ${pluralStringResource(R.plurals.added_days_ago, addedDaysAgo, addedDaysAgo)}",
            coverUrl = coverUrl,
            trailing = {
                Icon(
                    painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                    contentDescription = stringResource(R.string.content_desc_favorite),
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            // Only with a tag to show – an empty row would leave a gap under the author.
            chips = if (status == null && extraBadge == null) null else {
                { ListBookChips(status, extraBadge) }
            },
        )
    }
}

// The tags under the author: whether the book can be borrowed, and e.g. "Notify me".
@Composable
private fun RowScope.ListBookChips(status: ListStatus?, extraBadge: String?) {
    if (status != null) {
        StatusTag(
            text = stringResource(status.label),
            dotColor = status.color,
            backgroundColor = status.background,
            textColor = status.color,
        )
    }
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

// How much the user read in one year: the total, and the books per month for the bars.
private data class ReadingYear(val year: Int, val total: Int, val months: List<Pair<String, Int>>)

// TODO: the user's reading history from the server. Newest year first.
// The current year shows the last six months up to now; past years show July–December.
// Empty until then, and the card is hidden.
private val readingYears: List<ReadingYear> = emptyList()

@Composable
private fun ReadingStatsCard() {
    // Which year is shown. Starts at the newest one.
    var selectedYear by rememberSaveable { mutableIntStateOf(readingYears.first().year) }
    val stats = readingYears.first { it.year == selectedYear }
    // The month with the most books gets a full-height, dark bar; the others are measured against it.
    val most = stats.months.maxOf { it.second }.coerceAtLeast(1)

    ListCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.stats_books_read_in),
                    style = MaterialTheme.typography.titleMedium,
                )
                YearPicker(
                    selected = selectedYear,
                    years = readingYears.map { it.year },
                    onSelect = { selectedYear = it },
                )
                Spacer(Modifier.weight(1f))
                Text(stats.total.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
                stats.months.forEach { (_, books) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            // A month with no books still gets a thin line, so the gap doesn't look like a bug.
                            .fillMaxHeight((books.toFloat() / most).coerceAtLeast(0.04f))
                            .background(
                                if (books == most) MaterialTheme.colorScheme.primary
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
                stats.months.forEach { (label, _) ->
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

// The year in blue with a small arrow. Tapping it opens a list of years to pick from.
@Composable
private fun YearPicker(selected: Int, years: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    // Box, so the menu opens right under the year.
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(start = 6.dp, end = 2.dp),
        ) {
            Text(selected.toString(), style = MaterialTheme.typography.titleMedium)
            Icon(
                painterResource(R.drawable.ic_expand_more),
                contentDescription = stringResource(R.string.content_desc_pick_year),
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            year.toString(),
                            fontWeight = if (year == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(year)
                        expanded = false
                    },
                )
            }
        }
    }
}

// One of the user's books and how many times others borrowed it.
private data class BorrowedBook(val isbn: String, val title: String, val count: Int)

private val mostBorrowed: List<BorrowedBook> = emptyList()

@Composable
private fun MostBorrowedCard(onBookClick: (isbn: String) -> Unit) {
    // TODO: from the server – the user's books others borrowed most. Empty until then, and the card is hidden.
    val items = mostBorrowed

    ListCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.stats_most_borrowed), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { index, (isbn, title, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Rounded, so the gray flash on tap doesn't have square corners.
                        .clip(ButtonShape)
                        .clickable { onBookClick(isbn) }
                        .padding(vertical = 4.dp),
                ) {
                    Text("${index + 1}. $title", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
