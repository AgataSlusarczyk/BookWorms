package com.bookworms.app.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.PlaceholderGray

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
    Column {
        // Thin line that separates the bar from the screen content.
        HorizontalDivider(color = CardBorder)
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
                    onClick = { /* TODO: navigation */ },
                    icon = {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(id = titleResId), style = MaterialTheme.typography.labelSmall) },
                    // Like in the design: the selected item is just blue, without the pill behind the icon.
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

// Gray track with the selected tab shown as a white pill, like in the design.
@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(PlaceholderGray.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(4.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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