package com.bookworms.app.ui.book

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.AvatarPlaceholder
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.loans.LoanDays
import com.bookworms.app.ui.theme.AvailableGreen
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.FavoriteRed
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun BookDetailsScreen(
    book: BookDetails,
    isFavorite: Boolean,
    readingStatus: ReadingStatus,
    onFavoriteClick: () -> Unit,
    onReadingStatusChange: (ReadingStatus) -> Unit,
    // Owners the user already asked for this book, and owners whose copy the user waits for.
    requestedFrom: List<String>,
    queuedFor: List<String>,
    onSendRequest: (BookCopy) -> Unit,
    onCancelRequest: (BookCopy) -> Unit,
    onJoinQueue: (BookCopy) -> Unit,
    onLeaveQueue: (BookCopy) -> Unit,
    // The user's own lists, for "Add to list" in the ⋮ menu.
    customLists: List<String>,
    onShareClick: () -> Unit,
    onOwnBookClick: () -> Unit,
    // groupName is null for a public comment.
    onAddCommentClick: (groupName: String?) -> Unit,
    onReplyClick: (groupName: String, comment: BookComment) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Someone browsing without an account: they only read public comments.
    isGuest: Boolean = false,
    onLoginClick: () -> Unit = {},
) {
    val context = LocalContext.current

    // Which copy button was tapped, so the right question shows up. Kept as the owner's name,
    // so it survives turning the phone.
    var copyAction by rememberSaveable { mutableStateOf<CopyAction?>(null) }
    var copyOwner by rememberSaveable { mutableStateOf("") }
    var showListsDialog by rememberSaveable { mutableStateOf(false) }
    // TODO: the lists this book is on, from the server.
    var checkedLists by rememberSaveable(book.isbn) { mutableStateOf(listOf<String>()) }

    val dialogCopy = book.copies.find { it.ownerName == copyOwner }
    val action = copyAction
    if (action != null && dialogCopy != null) {
        CopyActionDialog(
            action = action,
            copy = dialogCopy,
            bookTitle = book.title,
            onConfirm = {
                when (action) {
                    CopyAction.ASK -> {
                        onSendRequest(dialogCopy)
                        Toast.makeText(context, context.getString(R.string.request_sent_to, dialogCopy.ownerName), Toast.LENGTH_SHORT).show()
                    }
                    CopyAction.CANCEL_REQUEST -> onCancelRequest(dialogCopy)
                    CopyAction.JOIN_QUEUE -> onJoinQueue(dialogCopy)
                    CopyAction.LEAVE_QUEUE -> onLeaveQueue(dialogCopy)
                }
                copyAction = null
            },
            onDismiss = { copyAction = null },
        )
    }

    if (showListsDialog) {
        AddToListDialog(
            lists = customLists,
            checked = checkedLists,
            onToggle = { name -> checkedLists = if (name in checkedLists) checkedLists - name else checkedLists + name },
            onDismiss = { showListsDialog = false },
        )
    }

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
            showUserActions = !isGuest,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick,
            onAddToListClick = { showListsDialog = true },
            onShareClick = onShareClick,
            onOwnBookClick = onOwnBookClick,
        )

        Spacer(Modifier.height(16.dp))

        BookHeader(book)

        Spacer(Modifier.height(16.dp))

        book.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionDivider()

        // A guest has no account to save a reading status to, and isn't in any group,
        // so instead of these two sections they get a reason to log in.
        if (isGuest) {
            GuestLoginCard(onLoginClick = onLoginClick)
        } else {
            SectionLabel(stringResource(R.string.reading_status_label))
            ReadingStatusSelector(selected = readingStatus, onSelect = onReadingStatusChange)

            SectionDivider()

            CopiesSection(
                book = book,
                requestedFrom = requestedFrom,
                queuedFor = queuedFor,
                onCopyButtonClick = { copy ->
                    // The same button asks or cancels, depending on what the user already did.
                    copyOwner = copy.ownerName
                    copyAction = when {
                        copy.status is CopyStatus.Available ->
                            if (copy.ownerName in requestedFrom) CopyAction.CANCEL_REQUEST else CopyAction.ASK
                        copy.ownerName in queuedFor -> CopyAction.LEAVE_QUEUE
                        else -> CopyAction.JOIN_QUEUE
                    }
                },
            )
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

// Back arrow on the left, heart and the ⋮ menu on the right.
// showUserActions is false for guests – without an account there's nowhere to save a favorite.
@Composable
private fun TopActions(
    isFavorite: Boolean,
    showUserActions: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToListClick: () -> Unit,
    onShareClick: () -> Unit,
    onOwnBookClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Moved by 12dp to the sides, so the icons line up with the content (same as BackTitle).
        IconButton(onClick = onBackClick, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
            )
        }
        Spacer(Modifier.weight(1f))
        if (!showUserActions) return@Row
        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = stringResource(
                    if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                ),
                tint = FavoriteRed,
            )
        }
        // The menu opens under the button, so both sit in one Box.
        Box(modifier = Modifier.offset(x = 12.dp)) {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(R.string.more_options),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_to_list)) },
                    onClick = { menuOpen = false; onAddToListClick() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_book)) },
                    onClick = { menuOpen = false; onShareClick() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.i_own_this_book)) },
                    onClick = { menuOpen = false; onOwnBookClick() },
                )
            }
        }
    }
}

// Light blue box for guests, in place of the copies: who has the book is only for people in the groups.
@Composable
private fun GuestLoginCard(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), FieldShape)
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.guest_book_title), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.guest_book_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLoginClick, shape = FieldShape, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.login_button))
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
            // "1961 · 224 pages · ISBN 978…", leaving out what the server doesn't know.
            val pages = book.pages?.let { pluralStringResource(R.plurals.book_pages, it, it) }
            Text(
                text = listOfNotNull(book.year?.toString(), pages, stringResource(R.string.isbn_value, book.isbn))
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Average of the public ratings, e.g. "★★★★☆ 4.5 · 2 ratings". Hidden until someone rates the book.
            book.averageRating()?.let { average ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(rating = average.roundToInt())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.average_rating,
                            "%.1f".format(average),
                            pluralStringResource(R.plurals.ratings_count, book.ratingCount(), book.ratingCount()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                book.genre?.let { genreName(it) }?.let { Pill(text = it) }
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

// "Who has it": one line saying where the book can be borrowed right now, then each of the
// user's groups with the copies in it. A copy can be free in one group and missing in another.
@Composable
private fun CopiesSection(
    book: BookDetails,
    requestedFrom: List<String>,
    queuedFor: List<String>,
    onCopyButtonClick: (BookCopy) -> Unit,
) {
    Text(stringResource(R.string.who_has_it), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))

    val availableIn = book.groupsWithAvailableCopy()
    Row(verticalAlignment = Alignment.CenterVertically) {
        StatusDot(if (availableIn.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else AvailableGreen)
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (availableIn.isEmpty()) {
                stringResource(R.string.not_available_now)
            } else {
                stringResource(R.string.available_now_in, availableIn.joinToString(", "))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    book.copiesByGroup().forEach { (groupName, copies) ->
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(groupName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.book_copies, copies.size, copies.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (copies.isEmpty()) {
            Text(
                text = stringResource(R.string.no_copies_in_group),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // All copies of one group in one card, with thin lines between them – shorter than a card each.
            OutlinedCard(
                shape = FieldShape,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                copies.forEachIndexed { index, copy ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    CopyRow(
                        copy = copy,
                        isRequested = copy.ownerName in requestedFrom,
                        isQueued = copy.ownerName in queuedFor,
                        onButtonClick = { onCopyButtonClick(copy) },
                    )
                }
            }
        }
    }
}

// One copy: who owns it, a colored line with its status, and a small button on the right.
// After the user asks or joins the queue, the button shows that, and tapping it again undoes it.
@Composable
private fun CopyRow(
    copy: BookCopy,
    isRequested: Boolean,
    isQueued: Boolean,
    onButtonClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
        AvatarPlaceholder(size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(copy.ownerName, style = MaterialTheme.typography.titleSmall)
            // e.g. "● Available · added 12.09" in green or "● Borrowed · due back 20.11" in blue.
            val (color, text) = when (val status = copy.status) {
                is CopyStatus.Available -> AvailableGreen to
                    "${stringResource(R.string.copy_available)} · ${stringResource(R.string.copy_added_on, status.addedOn)}"
                is CopyStatus.Borrowed -> MaterialTheme.colorScheme.primary to
                    "${stringResource(R.string.copy_borrowed)} · ${stringResource(R.string.copy_return_by, status.returnBy)}"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color)
                Spacer(Modifier.width(6.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
        Spacer(Modifier.width(8.dp))
        when (val status = copy.status) {
            is CopyStatus.Available -> if (isRequested) {
                // Gray – nothing to do but wait for the owner.
                OutlinedButton(
                    onClick = onButtonClick,
                    shape = FieldShape,
                    contentPadding = SmallButtonPadding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Text(stringResource(R.string.requested))
                }
            } else {
                Button(
                    onClick = onButtonClick,
                    shape = FieldShape,
                    contentPadding = SmallButtonPadding,
                ) {
                    Text(stringResource(R.string.borrow_action))
                }
            }

            is CopyStatus.Borrowed -> OutlinedButton(
                onClick = onButtonClick,
                shape = FieldShape,
                contentPadding = SmallButtonPadding,
                // Blue once the user is in the queue.
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isQueued) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(stringResource(if (isQueued) R.string.in_queue else R.string.join_queue_short, status.queuePosition))
            }
        }
    }
}

// What tapping a copy's button asks about.
private enum class CopyAction { ASK, CANCEL_REQUEST, JOIN_QUEUE, LEAVE_QUEUE }

@Composable
private fun CopyActionDialog(
    action: CopyAction,
    copy: BookCopy,
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val owner = copy.ownerName
    val borrowed = copy.status as? CopyStatus.Borrowed
    val title = when (action) {
        CopyAction.ASK -> stringResource(R.string.ask_for_book_title, owner, bookTitle)
        CopyAction.CANCEL_REQUEST -> stringResource(R.string.cancel_request_title)
        CopyAction.JOIN_QUEUE -> stringResource(R.string.join_queue_title)
        CopyAction.LEAVE_QUEUE -> stringResource(R.string.leave_queue_title)
    }
    val text = when (action) {
        CopyAction.ASK -> stringResource(R.string.ask_for_book_text, owner, LoanDays)
        CopyAction.CANCEL_REQUEST -> stringResource(R.string.cancel_request_text, owner)
        CopyAction.JOIN_QUEUE -> stringResource(
            R.string.join_queue_text,
            owner,
            borrowed?.returnBy.orEmpty(),
            borrowed?.queuePosition ?: 1,
        )
        CopyAction.LEAVE_QUEUE -> stringResource(R.string.leave_queue_text)
    }
    val confirm = when (action) {
        CopyAction.ASK -> R.string.send_action
        CopyAction.CANCEL_REQUEST -> R.string.cancel_request_action
        CopyAction.JOIN_QUEUE -> R.string.join_action
        CopyAction.LEAVE_QUEUE -> R.string.leave_action
    }
    // "Cancel" would be confusing next to "Cancel request", so the undo questions say "Keep" instead.
    val dismiss = when (action) {
        CopyAction.ASK, CopyAction.JOIN_QUEUE -> R.string.cancel
        CopyAction.CANCEL_REQUEST, CopyAction.LEAVE_QUEUE -> R.string.keep_action
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(dismiss)) } },
    )
}

// "Add to list" from the ⋮ menu: the user's lists with a check box each. Changes count right away.
@Composable
private fun AddToListDialog(
    lists: List<String>,
    checked: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_list)) },
        text = {
            if (lists.isEmpty()) {
                Text(stringResource(R.string.no_lists_yet))
            } else {
                Column {
                    lists.forEach { name ->
                        // The whole row is tappable, not only the small box.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(name) },
                        ) {
                            Checkbox(checked = name in checked, onCheckedChange = { onToggle(name) })
                            Spacer(Modifier.width(8.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done_action)) } },
    )
}

// Narrower than the default button padding, so the button fits next to the name.
private val SmallButtonPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
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
                comment.rating?.let { StarRow(rating = it, modifier = Modifier.padding(vertical = 2.dp)) }
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
            onReadingStatusChange = {},
            requestedFrom = emptyList(),
            queuedFor = listOf("Marek W."),
            onSendRequest = {},
            onCancelRequest = {},
            onJoinQueue = {},
            onLeaveQueue = {},
            customLists = listOf("Sci-fi", "Holiday reads"),
            onShareClick = {},
            onOwnBookClick = {},
            onAddCommentClick = {},
            onReplyClick = { _, _ -> },
            onBackClick = {},
        )
    }
}
