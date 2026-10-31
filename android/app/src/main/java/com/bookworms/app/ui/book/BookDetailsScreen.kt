package com.bookworms.app.ui.book

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.AvailableGreen
import com.bookworms.app.ui.theme.AvailableGreenLight
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.FavoriteRed
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun BookDetailsScreen(
    book: BookDetails,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
    onReadingStatusChange: (ReadingStatus) -> Unit,
    onRequestLoanClick: (BookCopy) -> Unit,
    onJoinQueueClick: (BookCopy) -> Unit,
    // groupName is null for a public comment.
    onAddCommentClick: (groupName: String?) -> Unit,
    onReplyClick: (groupName: String, comment: BookComment) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Someone browsing without an account: they only read public comments.
    isGuest: Boolean = false,
    onLoginClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            // Copies and comments make the screen long, so it scrolls.
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        TopActions(
            isFavorite = isFavorite,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick,
            onMoreClick = onMoreClick,
        )

        Spacer(Modifier.height(16.dp))

        BookHeader(book)

        Spacer(Modifier.height(16.dp))

        Text(
            text = book.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionDivider()

        SectionLabel(stringResource(R.string.reading_status_label))
        ReadingStatusSelector(selected = readingStatus, onSelect = onReadingStatusChange)

        SectionDivider()

        SectionLabel(stringResource(R.string.copies_in_group, book.copies.size))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            book.copies.forEach { copy ->
                CopyCard(
                    copy = copy,
                    onRequestLoanClick = { onRequestLoanClick(copy) },
                    onJoinQueueClick = { onJoinQueueClick(copy) },
                )
            }
        }

        SectionDivider()

        CommentsSection(
            book = book,
            isGuest = isGuest,
            onAddCommentClick = onAddCommentClick,
            onReplyClick = onReplyClick,
            onLoginClick = onLoginClick,
        )
    }
}

// Chips on top: "Public" and one for each of the user's groups. Below, the comments of the picked one.
@Composable
private fun CommentsSection(
    book: BookDetails,
    isGuest: Boolean,
    onAddCommentClick: (groupName: String?) -> Unit,
    onReplyClick: (groupName: String, comment: BookComment) -> Unit,
    onLoginClick: () -> Unit,
) {
    // 0 = Public, 1 and more = the groups in order. Guests only ever see Public.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val discussion = if (isGuest) null else book.groupDiscussions.getOrNull(selectedIndex - 1)
    val comments = discussion?.comments ?: book.publicComments

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.comments_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        // Guests can't write, so instead of "Add" they get a way to log in.
        // offset lines the text up with the right edge of the cards below.
        TextButton(
            onClick = { if (isGuest) onLoginClick() else onAddCommentClick(discussion?.groupName) },
            modifier = Modifier.offset(x = 12.dp),
        ) {
            Text(stringResource(if (isGuest) R.string.log_in_to_comment else R.string.add_comment))
        }
    }

    if (!isGuest) {
        // Scrolls sideways when someone is in many groups.
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = selectedIndex == 0,
                onClick = { selectedIndex = 0 },
                label = { Text(stringResource(R.string.comments_public, book.publicComments.size)) },
            )
            book.groupDiscussions.forEachIndexed { index, group ->
                FilterChip(
                    selected = selectedIndex == index + 1,
                    onClick = { selectedIndex = index + 1 },
                    label = { Text(stringResource(R.string.comments_group, group.groupName, group.comments.size)) },
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    if (comments.isEmpty()) {
        Text(
            text = if (discussion != null) {
                stringResource(R.string.no_group_discussion, discussion.groupName)
            } else {
                stringResource(R.string.no_public_comments)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        comments.forEach { comment ->
            CommentCard(comment)
            // Replies are only in group discussions: a bit to the right, under the comment they answer.
            if (discussion != null) {
                comment.replies.forEach { reply ->
                    CommentCard(reply, modifier = Modifier.padding(start = ReplyIndent))
                }
                TextButton(
                    onClick = { onReplyClick(discussion.groupName, comment) },
                    // Lines the text up with the replies above.
                    modifier = Modifier.padding(start = ReplyIndent - 12.dp),
                ) {
                    Text(stringResource(R.string.reply))
                }
            }
        }
    }
}

private val ReplyIndent = 40.dp

// Back arrow on the left, heart and "more" on the right.
@Composable
private fun TopActions(
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Moved by 12dp to the sides, so the icons line up with the content (same as BackTitle).
        IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = stringResource(
                    if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                ),
                tint = FavoriteRed,
            )
        }
        IconButton(onClick = onMoreClick, modifier = Modifier.offset(x = 12.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.more_options),
            )
        }
    }
}

// Cover, title, author, year/pages/ISBN and the gray labels.
@Composable
private fun BookHeader(book: BookDetails) {
    Row {
        // TODO: show the real cover when books come from the server.
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 136.dp)
                .background(PlaceholderGray, RoundedCornerShape(8.dp)),
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.book_meta,
                    book.year,
                    pluralStringResource(R.plurals.book_pages, book.pages, book.pages),
                    book.isbn,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(text = book.genre)
                Pill(text = pluralStringResource(R.plurals.book_copies, book.copies.size, book.copies.size))
            }
        }
    }
}

// Three buttons joined together; the picked one is white.
@Composable
private fun ReadingStatusSelector(selected: ReadingStatus, onSelect: (ReadingStatus) -> Unit) {
    val options = ReadingStatus.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, status ->
            SegmentedButton(
                selected = status == selected,
                onClick = { onSelect(status) },
                // Rounded corners only on the outer ends of the row.
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                // No check mark, the white fill already shows which one is picked.
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.surface,
                    activeContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // The same light border as the cards below.
                    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                label = { Text(stringResource(status.labelRes()), maxLines = 1) },
            )
        }
    }
}

@StringRes
private fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.TO_READ -> R.string.reading_status_to_read
    ReadingStatus.IN_PROGRESS -> R.string.reading_status_in_progress
    ReadingStatus.FINISHED -> R.string.reading_status_finished
}

// One copy: who owns it, whether it's free, and what the user can do about it.
@Composable
private fun CopyCard(
    copy: BookCopy,
    onRequestLoanClick: () -> Unit,
    onJoinQueueClick: () -> Unit,
) {
    OutlinedCard(
        shape = FieldShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = copy.ownerName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = when (val status = copy.status) {
                            is CopyStatus.Available -> stringResource(R.string.copy_added_on, status.addedOn)
                            is CopyStatus.Borrowed -> stringResource(R.string.copy_return_by, status.returnBy)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                CopyStatusPill(copy.status)
            }

            Spacer(Modifier.height(12.dp))

            when (val status = copy.status) {
                is CopyStatus.Available -> Button(
                    onClick = onRequestLoanClick,
                    shape = FieldShape,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.request_loan))
                }

                is CopyStatus.Borrowed -> OutlinedButton(
                    onClick = onJoinQueueClick,
                    shape = FieldShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.join_queue, status.queuePosition))
                }
            }
        }
    }
}

// Green "Available" or blue "Borrowed".
@Composable
private fun CopyStatusPill(status: CopyStatus) {
    when (status) {
        is CopyStatus.Available -> Pill(
            text = stringResource(R.string.copy_available),
            containerColor = AvailableGreenLight,
            contentColor = AvailableGreen,
            showDot = true,
        )

        is CopyStatus.Borrowed -> Pill(
            text = stringResource(R.string.copy_borrowed),
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary,
            showDot = true,
        )
    }
}

@Composable
private fun CommentCard(comment: BookComment, modifier: Modifier = Modifier) {
    // A comment with spoilers starts hidden; one tap shows it.
    var revealed by rememberSaveable(comment.id) { mutableStateOf(false) }
    val hidden = comment.hasSpoilers && !revealed

    OutlinedCard(
        shape = FieldShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AvatarPlaceholder()
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (hidden) {
                    Text(
                        text = stringResource(R.string.spoiler_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { revealed = true },
                    )
                } else {
                    Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pluralStringResource(R.plurals.days_ago, comment.daysAgo, comment.daysAgo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
}

// Small gray text above a section, e.g. "Your reading status".
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Preview(device = "id:pixel_8")
@Composable
private fun BookDetailsScreenPreview() {
    BookWormsTheme {
        BookDetailsScreen(
            book = sampleBookDetails,
            isFavorite = false,
            readingStatus = ReadingStatus.IN_PROGRESS,
            onFavoriteClick = {},
            onMoreClick = {},
            onReadingStatusChange = {},
            onRequestLoanClick = {},
            onJoinQueueClick = {},
            onAddCommentClick = {},
            onReplyClick = { _, _ -> },
            onBackClick = {},
        )
    }
}
