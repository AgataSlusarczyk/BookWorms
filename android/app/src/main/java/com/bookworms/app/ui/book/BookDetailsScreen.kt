package com.bookworms.app.ui.book

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.BookCover
import com.bookworms.app.ui.components.Pill
import com.bookworms.app.ui.components.OfflineBanner
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.loans.LoanDays
import com.bookworms.app.ui.theme.AvailableGreen
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.FavoriteRed
import com.bookworms.app.ui.theme.PlaceholderGray

@Composable
fun BookDetailsScreen(
    book: BookDetails,
    onFavoriteClick: () -> Unit,
    // null = the user took the status back by tapping the one already picked.
    onReadingStatusChange: (ReadingStatus?) -> Unit,
    // stars = 1–5, or null when the user takes the rating back.
    onRatingChange: (stars: Int?) -> Unit,
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
    // The copy whose button was just tapped, while the server answers. Its button is grayed out.
    busyCopyId: String? = null,
    // The server can't be reached: one of the user's own books, from the copy of My books on the phone.
    // Only the header and the description – no ratings, copies, comments or buttons.
    offline: Boolean = false,
    onRetryClick: () -> Unit = {},
    // A system admin also gets Edit book and Delete book in the ⋮ menu, and Hide under each public comment.
    isAdmin: Boolean = false,
    onAdminEditClick: () -> Unit = {},
    onAdminDeleteBook: () -> Unit = {},
    onAdminHideReview: (BookComment) -> Unit = {},
    // true while the server answers an admin's change – its buttons wait.
    adminBusy: Boolean = false,
) {
    // Which copy button was tapped, so the right question shows up. Kept as the copy's id,
    // so it survives turning the phone.
    var copyAction by rememberSaveable { mutableStateOf<CopyAction?>(null) }
    var copyId by rememberSaveable { mutableStateOf("") }
    var showListsDialog by rememberSaveable { mutableStateOf(false) }
    // TODO: the lists this book is on, from the server.
    var checkedLists by rememberSaveable(book.isbn) { mutableStateOf(listOf<String>()) }

    val dialogCopy = book.copies.find { it.id == copyId }
    val action = copyAction
    if (action != null && dialogCopy != null) {
        CopyActionDialog(
            action = action,
            copy = dialogCopy,
            bookTitle = book.title,
            onConfirm = {
                when (action) {
                    CopyAction.ASK -> onSendRequest(dialogCopy)
                    CopyAction.CANCEL_REQUEST -> onCancelRequest(dialogCopy)
                    CopyAction.JOIN_QUEUE -> onJoinQueue(dialogCopy)
                    CopyAction.LEAVE_QUEUE -> onLeaveQueue(dialogCopy)
                }
                copyAction = null
            },
            onDismiss = { copyAction = null },
        )
    }

    // The admin's questions before something that can't be undone.
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var hidingReviewId by rememberSaveable { mutableStateOf<String?>(null) }
    if (confirmDelete) {
        AdminConfirmDialog(
            title = stringResource(R.string.admin_delete_book_title, book.title),
            text = stringResource(R.string.admin_delete_book_text),
            confirm = stringResource(R.string.delete_action),
            onConfirm = {
                confirmDelete = false
                onAdminDeleteBook()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    book.publicComments.find { it.id == hidingReviewId }?.let { comment ->
        AdminConfirmDialog(
            title = stringResource(R.string.admin_hide_review_title, comment.authorName),
            text = stringResource(R.string.admin_hide_review_text),
            confirm = stringResource(R.string.admin_hide_review),
            onConfirm = {
                hidingReviewId = null
                onAdminHideReview(comment)
            },
            onDismiss = { hidingReviewId = null },
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
            isFavorite = book.isFavorite,
            showUserActions = !isGuest && !offline,
            onBackClick = onBackClick,
            onFavoriteClick = onFavoriteClick,
            onAddToListClick = { showListsDialog = true },
            onShareClick = onShareClick,
            onOwnBookClick = onOwnBookClick,
            showAdminActions = isAdmin && !isGuest && !offline,
            adminBusy = adminBusy,
            onAdminEditClick = onAdminEditClick,
            onAdminDeleteClick = { confirmDelete = true },
        )

        Spacer(Modifier.height(16.dp))

        if (offline) {
            OfflineBanner(onRetryClick)
            Spacer(Modifier.height(16.dp))
        }

        BookHeader(book, showRatings = !offline)

        Spacer(Modifier.height(16.dp))

        DescriptionSection(book)

        if (offline) return@Column

        SectionDivider()

        // A guest has no account to save a reading status to, and isn't in any group,
        // so instead of these two sections they get a reason to log in.
        if (isGuest) {
            GuestLoginCard(onLoginClick = onLoginClick)
        } else {
            SectionLabel(stringResource(R.string.reading_status_label))
            ReadingStatusSelector(selected = book.readingStatus, onSelect = onReadingStatusChange)

            Spacer(Modifier.height(16.dp))

            // Tapping a star saves it right away. Tapping the picked star again takes the rating back.
            SectionLabel(stringResource(R.string.your_book_rating_label))
            // offset lines the first star up with the text, because each star button has a bit of space around it.
            Row(modifier = Modifier.offset(x = (-12).dp)) {
                StarRatingPicker(rating = book.ratings.mine, onRatingChange = onRatingChange)
            }

            SectionDivider()

            CopiesSection(
                book = book,
                busyCopyId = busyCopyId,
                onCopyButtonClick = { copy ->
                    // The same button asks or cancels, depending on what the user already did.
                    copyId = copy.id
                    copyAction = when (val status = copy.status) {
                        is CopyStatus.Available ->
                            if (copy.myRequestId != null) CopyAction.CANCEL_REQUEST else CopyAction.ASK
                        else ->
                            if (status.queue()?.position != null) CopyAction.LEAVE_QUEUE else CopyAction.JOIN_QUEUE
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
            onHideClick = if (isAdmin && !isGuest && !adminBusy) {
                { comment -> hidingReviewId = comment.id }
            } else {
                null
            },
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
    // The admin's Hide under each public comment, or null for everyone else.
    onHideClick: ((BookComment) -> Unit)? = null,
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
            // Public comments are ratings, which an admin can hide. Group discussions aren't on the server yet.
            CommentCard(comment, onHideClick = onHideClick?.takeIf { discussion == null }?.let { { it(comment) } })
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
    showAdminActions: Boolean = false,
    adminBusy: Boolean = false,
    onAdminEditClick: () -> Unit = {},
    onAdminDeleteClick: () -> Unit = {},
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
                // Below a line, so it's clear these change the book for everyone, not only for the admin.
                if (showAdminActions) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.admin_edit_book)) },
                        enabled = !adminBusy,
                        onClick = { menuOpen = false; onAdminEditClick() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.admin_delete_book), color = MaterialTheme.colorScheme.error) },
                        enabled = !adminBusy,
                        onClick = { menuOpen = false; onAdminDeleteClick() },
                    )
                }
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

// The user's own description when they wrote one, with a link to the catalog's – or just the catalog's.
@Composable
private fun DescriptionSection(book: BookDetails) {
    var showCatalog by rememberSaveable(book.isbn) { mutableStateOf(false) }
    val mine = book.myDescription
    val text = if (mine != null && !showCatalog) mine else book.description
    if (mine != null) {
        Text(
            text = stringResource(if (showCatalog) R.string.catalog_description_label else R.string.my_description_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
    }
    text?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Only when there's something to switch to.
    if (mine != null && book.description != null) {
        // offset lines the text up with the description, because the button has a bit of space around it.
        TextButton(onClick = { showCatalog = !showCatalog }, modifier = Modifier.offset(x = (-12).dp)) {
            Text(stringResource(if (showCatalog) R.string.show_my_description else R.string.show_catalog_description))
        }
    }
}

// Cover, title, author, year/pages/ISBN and the gray labels.
// showRatings = false without the server – the ratings aren't saved on the phone.
@Composable
private fun BookHeader(book: BookDetails, showRatings: Boolean = true) {
    Row {
        BookCover(book.coverUrl, Modifier.size(width = 96.dp, height = 136.dp))

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
            // Average of everyone's ratings, e.g. "★★★★☆ 4.5 · 2 ratings", or "No ratings yet".
            Spacer(Modifier.height(8.dp))
            val average = book.ratings.average
            if (!showRatings) {
                // Nothing – see above.
            } else if (average == null) {
                Text(
                    text = stringResource(R.string.no_ratings_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(rating = average.roundToInt())
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.average_rating,
                            "%.1f".format(average),
                            pluralStringResource(R.plurals.ratings_count, book.ratings.count, book.ratings.count),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                book.genre?.let { genreName(it) }?.let { Pill(text = it) }
                // The user's own copies count too – they're copies in the user's groups like the others.
                val copyCount = book.copies.size + book.ownCopies
                Pill(text = pluralStringResource(R.plurals.book_copies, copyCount, copyCount))
            }
        }
    }
}

// Three buttons joined together; the picked one is white.
@Composable
// Nothing is picked at first. Tapping the picked one again takes it back (onSelect(null)).
private fun ReadingStatusSelector(selected: ReadingStatus?, onSelect: (ReadingStatus?) -> Unit) {
    val options = ReadingStatus.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, status ->
            SegmentedButton(
                selected = status == selected,
                onClick = { onSelect(if (status == selected) null else status) },
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
    busyCopyId: String?,
    onCopyButtonClick: (BookCopy) -> Unit,
) {
    Text(stringResource(R.string.who_has_it), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))

    // The user's own copy first: "You have this book", so it's clear the app knows.
    if (book.ownCopies > 0) {
        Text(
            text = pluralStringResource(R.plurals.you_have_this_book, book.ownCopies, book.ownCopies),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
    }

    if (book.copies.isEmpty()) {
        Text(
            text = stringResource(if (book.ownCopies > 0) R.string.no_other_copies_yet else R.string.no_copies_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

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
                        isBusy = copy.id == busyCopyId,
                        onButtonClick = { onCopyButtonClick(copy) },
                    )
                }
            }
        }
    }
}

// One copy: who owns it, a colored line with its status, and a small button on the right.
// After the user asks or joins the queue, the button shows that, and tapping it again undoes it.
// isBusy = the server is answering the last tap, so the button waits.
@Composable
private fun CopyRow(
    copy: BookCopy,
    isBusy: Boolean,
    onButtonClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
        ProfilePhoto(photo = copy.ownerPhotoUrl, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(copy.ownerName, style = MaterialTheme.typography.titleSmall)
            // e.g. "● Available · added 12.09" in green or "● Borrowed · due back 20.11" in blue.
            val (color, text) = when (val status = copy.status) {
                is CopyStatus.Available -> AvailableGreen to
                    "${stringResource(R.string.copy_available)} · ${stringResource(R.string.copy_added_on, status.addedOn.short())}"
                is CopyStatus.Borrowed -> MaterialTheme.colorScheme.primary to
                    "${stringResource(if (status.byMe) R.string.copy_borrowed_by_you else R.string.copy_borrowed)} · " +
                    stringResource(R.string.copy_return_by, status.returnBy.short())
                is CopyStatus.NotLending -> MaterialTheme.colorScheme.onSurfaceVariant to
                    stringResource(R.string.copy_not_lending)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color)
                Spacer(Modifier.width(6.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
        val status = copy.status
        val queue = status.queue()
        // The user has it themselves – nothing to ask for or wait for.
        if (status is CopyStatus.Borrowed && status.byMe) return@Row
        Spacer(Modifier.width(8.dp))
        when {
            status is CopyStatus.Available -> if (copy.myRequestId != null) {
                // Gray – nothing to do but wait for the owner.
                OutlinedButton(
                    onClick = onButtonClick,
                    enabled = !isBusy,
                    shape = FieldShape,
                    contentPadding = SmallButtonPadding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Text(stringResource(R.string.requested))
                }
            } else {
                Button(
                    onClick = onButtonClick,
                    enabled = !isBusy,
                    shape = FieldShape,
                    contentPadding = SmallButtonPadding,
                ) {
                    Text(stringResource(R.string.borrow_action))
                }
            }

            queue != null -> OutlinedButton(
                onClick = onButtonClick,
                enabled = !isBusy,
                shape = FieldShape,
                contentPadding = SmallButtonPadding,
                // Blue once the user is in the queue.
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (queue.position != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    stringResource(
                        if (queue.position != null) R.string.in_queue else R.string.join_queue_short,
                        queue.placeForUser,
                    ),
                )
            }
        }
    }
}

// The queue of a copy that can't be borrowed right now, or null for an available one.
private fun CopyStatus.queue(): Queue? = when (this) {
    is CopyStatus.Available -> null
    is CopyStatus.Borrowed -> queue
    is CopyStatus.NotLending -> queue
}

private val DayMonth = DateTimeFormatter.ofPattern("dd.MM")

// e.g. "20.11".
private fun LocalDate.short(): String = format(DayMonth)

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
    val status = copy.status
    val place = status.queue()?.placeForUser ?: 1
    val title = when (action) {
        CopyAction.ASK -> stringResource(R.string.ask_for_book_title, owner, bookTitle)
        CopyAction.CANCEL_REQUEST -> stringResource(R.string.cancel_request_title)
        CopyAction.JOIN_QUEUE -> stringResource(R.string.join_queue_title)
        CopyAction.LEAVE_QUEUE -> stringResource(R.string.leave_queue_title)
    }
    val text = when (action) {
        CopyAction.ASK -> stringResource(R.string.ask_for_book_text, owner, LoanDays)
        CopyAction.CANCEL_REQUEST -> stringResource(R.string.cancel_request_text, owner)
        CopyAction.JOIN_QUEUE -> if (status is CopyStatus.Borrowed) {
            stringResource(R.string.join_queue_text, owner, status.returnBy.short(), place)
        } else {
            stringResource(R.string.join_queue_not_lending_text, owner, place)
        }
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
private fun AdminConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// onHideClick = the admin's Hide in the corner, or null for everyone else.
@Composable
private fun CommentCard(comment: BookComment, modifier: Modifier = Modifier, onHideClick: (() -> Unit)? = null) {
    // A comment with spoilers starts hidden; one tap shows it.
    var revealed by rememberSaveable(comment.id) { mutableStateOf(false) }
    val hidden = comment.hasSpoilers && !revealed

    OutlinedCard(
        shape = FieldShape,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            ProfilePhoto(photo = comment.authorPhotoUrl, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            if (onHideClick != null) {
                TextButton(onClick = onHideClick, contentPadding = SmallButtonPadding) {
                    Text(stringResource(R.string.admin_hide_review), color = MaterialTheme.colorScheme.error)
                }
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
            onFavoriteClick = {},
            onReadingStatusChange = {},
            onRatingChange = {},
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
