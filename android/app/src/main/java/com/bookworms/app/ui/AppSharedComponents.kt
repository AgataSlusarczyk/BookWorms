package com.bookworms.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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

val CardShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(8.dp)
val ScreenTitleSize = 24.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text(title, fontSize = ScreenTitleSize, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        HorizontalDivider(color = CardBorder)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            shape = CardShape,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorder),
            modifier = modifier.fillMaxWidth(),
            content = content
        )
    } else {
        OutlinedCard(
            shape = CardShape,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, CardBorder),
            modifier = modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
fun BookRow(title: String, author: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BookCoverPlaceholder()
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun AppBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = CardBorder)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple("library", R.string.nav_library, R.drawable.ic_library),
                Triple("loans", R.string.nav_loans, R.drawable.ic_loans),
                Triple("groups", R.string.nav_groups, R.drawable.ic_group),
                Triple("lists", R.string.nav_my_lists, R.drawable.ic_lists),
                Triple("profile", R.string.nav_profile, R.drawable.ic_profile)
            )
            
            items.forEach { (route, titleResId, iconResId) ->
                NavigationBarItem(
                    selected = route == currentRoute,
                    onClick = { onNavigate(route) },
                    icon = { Icon(painterResource(iconResId), contentDescription = null) },
                    label = { Text(stringResource(titleResId)) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun StatusTag(
    text: String, 
    dotColor: Color = MaterialTheme.colorScheme.primary, 
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant, 
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
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
            Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Medium)
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

@Composable
fun SegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(index) },
                            role = Role.Tab
                        ),
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