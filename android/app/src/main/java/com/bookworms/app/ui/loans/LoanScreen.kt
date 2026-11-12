package com.bookworms.app.ui.loans

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState
import com.bookworms.app.ui.components.*
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.StatusAmber
import com.bookworms.app.ui.theme.StatusAmberBg
import com.bookworms.app.ui.theme.StatusBlueBg
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dayMonth = DateTimeFormatter.ofPattern("dd.MM")

@Composable
internal fun LoansScreen(
    loans: LoadState<Loans>,
    // true while an answer or "Mark as returned" is on its way – the buttons wait for it.
    saving: Boolean,
    onRetryClick: () -> Unit,
    onMarkReturned: (Loan) -> Unit,
    onAccept: (BookRequest) -> Unit,
    onReject: (BookRequest) -> Unit,
    onNavigate: (String) -> Unit = {},
    // Gets the book's ISBN. Books typed in by hand have none, so their cards don't open anything.
    onBookClick: (isbn: String) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_borrowing),
        stringResource(R.string.tab_lent),
        stringResource(R.string.tab_requests),
    )

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.loans_title)) },
        bottomBar = { AppBottomBar(currentRoute = "loans", onNavigate = onNavigate) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                SegmentedTabs(
                    tabs = tabs,
                    selectedIndex = selectedTabIndex,
                    onSelect = { selectedTabIndex = it },
                )
            }

            when (loans) {
                LoadState.Loading -> item { LoadingIndicator() }
                LoadState.Failed -> item { LoadFailedMessage(stringResource(R.string.load_loans_failed), onRetryClick) }
                is LoadState.Ready -> {
                    val data = loans.data
                    val openBook = { isbn: String? -> isbn?.let { { onBookClick(it) } } }
                    when (selectedTabIndex) {
                        // Books the user has from others, and the ones they asked for.
                        0 -> {
                            if (data.borrowed.isEmpty() && data.sentRequests.isEmpty()) {
                                emptyText(R.string.loans_empty_borrowing)
                            }
                            if (data.borrowed.isNotEmpty()) {
                                item { SectionHeader(stringResource(R.string.section_we_have, data.borrowed.size.toString())) }
                                items(data.borrowed, key = { it.id }) { loan ->
                                    LoanCard(loan, R.string.loan_subtitle, onClick = openBook(loan.isbn))
                                }
                            }
                            if (data.sentRequests.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        stringResource(R.string.section_waiting_for_answer, data.sentRequests.size.toString()),
                                        Modifier.padding(top = 4.dp),
                                    )
                                }
                                items(data.sentRequests, key = { it.id }) { request ->
                                    RequestCard(
                                        request,
                                        subtitle = stringResource(R.string.you_asked, request.person),
                                        onClick = openBook(request.isbn),
                                    )
                                }
                            }
                        }

                        // The user's books that others have now.
                        1 -> {
                            if (data.lent.isEmpty()) {
                                emptyText(R.string.loans_empty_lent)
                            } else {
                                item { SectionHeader(stringResource(R.string.section_lent_out, data.lent.size.toString())) }
                                items(data.lent, key = { it.id }) { loan ->
                                    LoanCard(
                                        loan,
                                        R.string.loan_subtitle_lent,
                                        onClick = openBook(loan.isbn),
                                        onReturnedClick = { onMarkReturned(loan) },
                                        buttonsEnabled = !saving,
                                    )
                                }
                            }
                        }

                        // Others asking to borrow the user's books.
                        else -> {
                            if (data.receivedRequests.isEmpty()) {
                                emptyText(R.string.loans_empty_requests)
                            } else {
                                item {
                                    SectionHeader(stringResource(R.string.section_pending_confirmation, data.receivedRequests.size.toString()))
                                }
                                items(data.receivedRequests, key = { it.id }) { request ->
                                    RequestCard(
                                        request,
                                        subtitle = stringResource(R.string.asks_to_borrow, request.person),
                                        onClick = openBook(request.isbn),
                                        onAccept = { onAccept(request) },
                                        onReject = { onReject(request) },
                                        buttonsEnabled = !saving,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Gray text when a tab has nothing to show.
private fun LazyListScope.emptyText(@StringRes text: Int) {
    item {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// A borrowed or lent book: blue chip with the return date, or a red chip and border when it's late.
// onReturnedClick adds a "Mark as returned" button – only the owner (Lent) gets it.
@Composable
private fun LoanCard(
    loan: Loan,
    @StringRes subtitle: Int,
    onClick: (() -> Unit)?,
    onReturnedClick: (() -> Unit)? = null,
    buttonsEnabled: Boolean = true,
) {
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), loan.returnBy).toInt()
    val isOverdue = daysLeft < 0
    val error = MaterialTheme.colorScheme.error

    AppCard(borderColor = if (isOverdue) error else CardBorder, onClick = onClick) {
        BookRow(
            title = loan.title,
            subtitle = stringResource(subtitle, loan.person, loan.since.format(dayMonth)),
            coverUrl = loan.coverUrl,
        ) {
            if (isOverdue) {
                StatusTag(stringResource(R.string.status_overdue), dotColor = error, backgroundColor = MaterialTheme.colorScheme.errorContainer, textColor = error)
            } else {
                StatusTag(
                    stringResource(R.string.status_return_by, loan.returnBy.format(dayMonth)),
                    dotColor = BrandBlue,
                    backgroundColor = StatusBlueBg,
                    textColor = BrandBlue,
                )
            }
            val extraText = when {
                isOverdue -> pluralStringResource(R.plurals.x_days_overdue, -daysLeft, -daysLeft)
                daysLeft == 0 -> stringResource(R.string.due_today)
                else -> pluralStringResource(R.plurals.in_x_days, daysLeft, daysLeft)
            }
            Text(extraText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onReturnedClick != null) {
            OutlinedButton(
                onClick = onReturnedClick,
                enabled = buttonsEnabled,
                shape = ButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                Text(stringResource(R.string.mark_returned))
            }
        }
    }
}

// A request waiting for an answer: amber "Pending" chip.
// With onAccept and onReject (requests to the user) it also has Reject / Accept buttons.
// Accept is grayed out while the book is lent out or the user made it not available.
// Tapping the buttons doesn't open the book – only the rest of the card does.
@Composable
private fun RequestCard(
    request: BookRequest,
    subtitle: String,
    onClick: (() -> Unit)?,
    onAccept: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
    buttonsEnabled: Boolean = true,
) {
    AppCard(onClick = onClick) {
        BookRow(title = request.title, subtitle = subtitle, coverUrl = request.coverUrl) {
            StatusTag(stringResource(R.string.status_pending), dotColor = StatusAmber, backgroundColor = StatusAmberBg, textColor = StatusAmber)
            Text(
                timeAgo(request.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onAccept != null && onReject != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                OutlinedButton(onClick = onReject, enabled = buttonsEnabled, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_reject))
                }
                Button(
                    onClick = onAccept,
                    enabled = buttonsEnabled && request.canAccept,
                    shape = ButtonShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_accept))
                }
            }
        }
    }
}

// "Just now", "5 h ago" or "2 days ago".
@Composable
private fun timeAgo(moment: Instant): String {
    val hours = Duration.between(moment, Instant.now()).toHours()
    return when {
        hours < 1 -> stringResource(R.string.just_now)
        hours < 24 -> stringResource(R.string.x_hours_ago, hours.toString())
        else -> (hours / 24).toInt().let { days -> pluralStringResource(R.plurals.days_ago, days, days) }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun LoansScreenPreview() {
    val today = LocalDate.now()
    BookWormsTheme {
        LoansScreen(
            loans = LoadState.Ready(
                Loans(
                    borrowed = listOf(
                        Loan("1", "Problem trzech ciał", null, null, "Marek W.", today.minusDays(8), today.plusDays(6)),
                        Loan("2", "Pachnidło", null, null, "Ola N.", today.minusDays(30), today.minusDays(4)),
                    ),
                    sentRequests = listOf(
                        BookRequest("3", "Bieguni", null, null, "Anna K.", Instant.now().minusSeconds(5 * 3600), canAccept = false),
                    ),
                    lent = emptyList(),
                    receivedRequests = emptyList(),
                ),
            ),
            saving = false,
            onRetryClick = {},
            onMarkReturned = {},
            onAccept = {},
            onReject = {},
        )
    }
}
