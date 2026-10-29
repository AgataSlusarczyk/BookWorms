package com.bookworms.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.PlaceholderGray

// Pieces shared by all screens inside the app, so every screen looks the same.

val CardShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(8.dp)

// Size of every screen title, so titles look the same everywhere (top bars, login, sign-up).
val ScreenTitleSize = 18.sp

// Top bar of every screen: bold title, a back arrow if the screen has one, optional buttons on the
// right, and a thin line underneath. TopAppBar keeps itself below the status bar (clock, battery).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column {
        TopAppBar(
            title = { Text(title, fontSize = ScreenTitleSize, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                }
            },
            actions = actions,
            // Transparent, so the misted picture shows through.
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        HorizontalDivider(color = CardBorder)
    }
}

// White card with a thin light border – the base for every card in the app.
// Pass onClick to make the whole card tappable.
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, borderColor)
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        OutlinedCard(onClick = onClick, shape = CardShape, border = border, colors = colors, modifier = modifier.fillMaxWidth(), content = content)
    } else {
        OutlinedCard(shape = CardShape, border = border, colors = colors, modifier = modifier.fillMaxWidth(), content = content)
    }
}

// Small gray text above a group of cards, e.g. "Today" or "4 books".
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// Gray box in place of a book cover until we have real covers.
@Composable
fun BookCoverPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 64.dp)
            .background(PlaceholderGray, RoundedCornerShape(6.dp))
    )
}

// Card row with a cover, title, a gray line under it, and optional chips at the bottom.
// Used for books on Library, Loans, My lists and My books.
@Composable
fun BookRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    chips: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(12.dp)) {
        BookCoverPlaceholder()
        Spacer(Modifier.width(12.dp))
        // weight(1f) takes the free space, so anything in trailing goes to the right edge.
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (chips != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, content = chips)
            }
        }
        trailing()
    }
}

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Surface(
        color = PlaceholderGray.copy(alpha = 0.7f),
        shape = CardShape,
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = ButtonShape,
                        )
                        // Role.Tab makes screen readers announce these as tabs, not plain buttons.
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun AppBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("library", R.string.nav_library, R.drawable.ic_library),
        Triple("loans", R.string.nav_loans, R.drawable.ic_loans),
        Triple("lists", R.string.nav_my_lists, R.drawable.ic_lists),
        Triple("profile", R.string.nav_profile, R.drawable.ic_profile)
    )

    Column {
        // Thin line above the bar, the same as under the top bar.
        HorizontalDivider(color = CardBorder)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            items.forEach { (route, titleResId, iconResId) ->
                NavigationBarItem(
                    selected = route == currentRoute,
                    onClick = { onNavigate(route) },
                    icon = { Icon(painter = painterResource(iconResId), contentDescription = null) },
                    label = { Text(stringResource(titleResId)) },
                    // The chosen tab is just blue – no colored pill behind the icon.
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
fun StatusTag(
    text: String,
    dotColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = PlaceholderGray,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}
