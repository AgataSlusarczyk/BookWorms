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
import com.bookworms.app.ui.components.*
import com.bookworms.app.ui.theme.BookWormsTheme
import com.bookworms.app.ui.theme.BrandBlue
import com.bookworms.app.ui.theme.CardBorder
import com.bookworms.app.ui.theme.StatusAmber
import com.bookworms.app.ui.theme.StatusAmberBg
import com.bookworms.app.ui.theme.StatusBlueBg
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// A book that changed hands. person = who it's from (Borrowing) or who has it (Lent).
private data class Loan(val id: Int, val title: String, val person: String, val since: LocalDate, val returnBy: LocalDate)

// Someone asked to borrow a book. person = who asked (Requests) or whose book it is (Borrowing).
private data class LoanRequest(val id: Int, val title: String, val person: String, val hoursAgo: Int)

// How long a book is lent for when the user accepts a request.
private const val LoanDays = 14L

// TODO: the user's loans and requests from the server.
// Dates are counted from today, so the sample stays the same whenever the app is opened.
private val today: LocalDate get() = LocalDate.now()

private val sampleBorrowed = listOf(
    Loan(1, "Problem trzech ciał", "Marek W.", today.minusDays(8), today.plusDays(6)),
    Loan(2, "Pachnidło", "Ola N.", today.minusDays(30), today.minusDays(4)),
)

private val sampleSentRequests = listOf(
    LoanRequest(11, "Bieguni", "Anna K.", hoursAgo = 5),
)

private val sampleLent = listOf(
    Loan(21, "Kwiaty dla Algernona", "Kasia M.", today.minusDays(3), today.plusDays(11)),
    Loan(22, "Wiedźmin", "Tomek R.", today.minusDays(20), today.minusDays(2)),
)

private val sampleRequests = listOf(
    LoanRequest(31, "Solaris", "Krzysiek P.", hoursAgo = 2),
    LoanRequest(32, "Lalka", "Ola N.", hoursAgo = 26),
)

private val dayMonth = DateTimeFormatter.ofPattern("dd.MM")

@Composable
fun LoansScreen(
    onNavigate: (String) -> Unit = {},
    // Gets the book's title – the sample books here have no ISBN yet.
    onBookClick: (title: String) -> Unit = {},
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_borrowing),
        stringResource(R.string.tab_lent),
        stringResource(R.string.tab_requests),
    )

    // What the user did on this screen, kept as ids so it survives turning the phone.
    // TODO: send each answer to the server instead.
    var acceptedIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var rejectedIds by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var returnedIds by rememberSaveable { mutableStateOf(listOf<Int>()) }

    val requests = sampleRequests.filter { it.id !in acceptedIds && it.id !in rejectedIds }
    // An accepted request becomes a book the user lent out, from today.
    val lent = (sampleRequests.filter { it.id in acceptedIds }
        .map { Loan(it.id, it.title, it.person, today, today.plusDays(LoanDays)) } + sampleLent)
        .filter { it.id !in returnedIds }

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

            when (selectedTabIndex) {
                // Books the user has from others, and the ones they asked for.
                0 -> {
                    if (sampleBorrowed.isEmpty() && sampleSentRequests.isEmpty()) {
                        emptyText(R.string.loans_empty_borrowing)
                    }
                    if (sampleBorrowed.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_we_have, sampleBorrowed.size.toString())) }
                        items(sampleBorrowed, key = { it.id }) { loan ->
                            LoanCard(loan, R.string.loan_subtitle, onClick = { onBookClick(loan.title) })
                        }
                    }
                    if (sampleSentRequests.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_waiting_for_answer, sampleSentRequests.size.toString()), Modifier.padding(top = 4.dp)) }
                        items(sampleSentRequests, key = { it.id }) { request ->
                            RequestCard(
                                request,
                                subtitle = stringResource(R.string.you_asked, request.person),
                                onClick = { onBookClick(request.title) },
                            )
                        }
                    }
                }

                // The user's books that others have now.
                1 -> {
                    if (lent.isEmpty()) {
                        emptyText(R.string.loans_empty_lent)
                    } else {
                        item { SectionHeader(stringResource(R.string.section_lent_out, lent.size.toString())) }
                        items(lent, key = { it.id }) { loan ->
                            LoanCard(
                                loan,
                                R.string.loan_subtitle_lent,
                                onClick = { onBookClick(loan.title) },
                                onReturnedClick = { returnedIds = returnedIds + loan.id },
                            )
                        }
                    }
                }

                // Others asking to borrow the user's books.
                else -> {
                    if (requests.isEmpty()) {
                        emptyText(R.string.loans_empty_requests)
                    } else {
                        item { SectionHeader(stringResource(R.string.section_pending_confirmation, requests.size.toString())) }
                        items(requests, key = { it.id }) { request ->
                            RequestCard(
                                request,
                                subtitle = stringResource(R.string.asks_to_borrow, request.person),
                                onClick = { onBookClick(request.title) },
                                onAccept = { acceptedIds = acceptedIds + request.id },
                                onReject = { rejectedIds = rejectedIds + request.id },
                            )
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
    onClick: () -> Unit,
    onReturnedClick: (() -> Unit)? = null,
) {
    val daysLeft = ChronoUnit.DAYS.between(today, loan.returnBy).toInt()
    val isOverdue = daysLeft < 0
    val error = MaterialTheme.colorScheme.error

    AppCard(borderColor = if (isOverdue) error else CardBorder, onClick = onClick) {
        BookRow(title = loan.title, subtitle = stringResource(subtitle, loan.person, loan.since.format(dayMonth))) {
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
// Tapping the buttons doesn't open the book – only the rest of the card does.
@Composable
private fun RequestCard(
    request: LoanRequest,
    subtitle: String,
    onClick: () -> Unit,
    onAccept: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null,
) {
    AppCard(onClick = onClick) {
        BookRow(title = request.title, subtitle = subtitle) {
            StatusTag(stringResource(R.string.status_pending), dotColor = StatusAmber, backgroundColor = StatusAmberBg, textColor = StatusAmber)
            Text(
                stringResource(R.string.x_hours_ago, request.hoursAgo.toString()),
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
                OutlinedButton(onClick = onReject, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_reject))
                }
                Button(onClick = onAccept, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_accept))
                }
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun LoansScreenPreview() {
    BookWormsTheme {
        LoansScreen()
    }
}
