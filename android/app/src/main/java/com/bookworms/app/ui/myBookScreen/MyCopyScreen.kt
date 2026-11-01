package com.bookworms.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppCard
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.BookCoverPlaceholder
import com.bookworms.app.ui.components.SectionHeader
import com.bookworms.app.ui.components.StatusTag
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.StatusBlueBg
import com.bookworms.app.ui.theme.StatusGreen
import com.bookworms.app.ui.theme.StatusGreenBg

// Your own copy of a book: where it is, which groups can see it, and edit / remove.
// The book's page for everyone (comments, other copies) is one tap away.
@Composable
fun MyCopyScreen(
    title: String,
    onEditClick: () -> Unit,
    onOpenBookPageClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val book = ownBook(title)
    // TODO: save each change on the server. For now they last until the screen is closed.
    var visibleIn by rememberSaveable { mutableStateOf(book.groups) }
    var availableToBorrow by rememberSaveable { mutableStateOf(true) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(title = stringResource(R.string.my_copy_title), onBackClick = onBackClick) {
                IconButton(onClick = onEditClick) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.edit_book_title))
                }
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            BookHeader(book)
            WhereItIsCard(book)

            SectionHeader(stringResource(R.string.visible_in), Modifier.padding(top = 4.dp))
            AppCard {
                myGroups.forEachIndexed { index, group ->
                    if (index > 0) HorizontalDivider(color = CardBorder)
                    SwitchRow(
                        text = group,
                        checked = group in visibleIn,
                        onCheckedChange = { checked -> visibleIn = if (checked) visibleIn + group else visibleIn - group },
                    )
                }
            }
            HintText(stringResource(if (visibleIn.isEmpty()) R.string.visible_to_nobody else R.string.visible_in_hint))

            AppCard {
                SwitchRow(
                    text = stringResource(R.string.available_to_borrow),
                    subtitle = stringResource(R.string.available_to_borrow_hint),
                    checked = availableToBorrow,
                    onCheckedChange = { availableToBorrow = it },
                )
            }

            AppCard(onClick = onOpenBookPageClick) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.open_book_page), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // A book someone has right now can't be removed – it has to come back first.
            TextButton(
                onClick = { showRemoveDialog = true },
                enabled = book.lentTo == null,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.remove_from_my_books))
            }
            if (book.lentTo != null) {
                HintText(stringResource(R.string.remove_lent_hint), Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    // Asks first, because a removed book can't be brought back.
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.remove_book_title, book.title)) },
            text = { Text(stringResource(R.string.remove_book_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        // TODO: remove the book on the server first.
                        onRemoveClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.remove_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

// Bigger cover, title, "author · year" and the ISBN.
@Composable
private fun BookHeader(book: OwnBook) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BookCoverPlaceholder(Modifier.size(width = 72.dp, height = 96.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(book.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.book_author_year, book.author, book.year),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.isbn_value, book.isbn),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Green "On your shelf", or blue "Lent to Marek W." with the dates underneath.
@Composable
private fun WhereItIsCard(book: OwnBook) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(16.dp)) {
            if (book.lentTo == null) {
                StatusTag(stringResource(R.string.on_your_shelf), dotColor = StatusGreen, backgroundColor = StatusGreenBg, textColor = StatusGreen)
            } else {
                StatusTag(stringResource(R.string.status_lent_to, book.lentTo), dotColor = BrandBlue, backgroundColor = StatusBlueBg, textColor = BrandBlue)
                if (book.lentSince != null && book.returnBy != null) {
                    HintText(stringResource(R.string.lent_dates, book.lentSince, book.returnBy))
                }
            }
        }
    }
}

// Text on the left (with an optional gray line under it), on/off switch on the right.
@Composable
private fun SwitchRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) HintText(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

@Preview(device = "id:pixel_8")
@Composable
private fun MyCopyScreenPreview() {
    BookWormsTheme {
        MyCopyScreen(title = "Problem trzech ciał", onEditClick = {}, onOpenBookPageClick = {}, onRemoveClick = {}, onBackClick = {})
    }
}
