package com.bookworms.app.ui.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun LoansScreen(onNavigate: (String) -> Unit = {}) {
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

            item { SectionHeader(stringResource(R.string.section_we_have, "2")) }
            item {
                LoanCard(
                    title = "Problem trzech ciał",
                    subtitle = stringResource(R.string.loan_subtitle, "Marek W.", "12.11"),
                    statusText = stringResource(R.string.status_return_by, "20.11"),
                    extraText = pluralStringResource(R.plurals.in_x_days, 6, 6),
                )
            }
            item {
                LoanCard(
                    title = "Pachnidło",
                    subtitle = stringResource(R.string.loan_subtitle, "Ola N.", "03.10"),
                    statusText = stringResource(R.string.status_overdue),
                    extraText = pluralStringResource(R.plurals.x_days_overdue, 4, 4),
                    isOverdue = true,
                )
            }

            item { Spacer(Modifier.height(4.dp)) }
            item { SectionHeader(stringResource(R.string.section_pending_confirmation, "1")) }
            item {
                RequestCard(
                    title = "Solaris",
                    subtitle = stringResource(R.string.asks_to_borrow, "Krzysiek P."),
                    time = stringResource(R.string.x_hours_ago, "2"),
                )
            }
        }
    }
}

// A book you borrowed: blue chip with the return date, or a red chip and border when it's late.
@Composable
private fun LoanCard(title: String, subtitle: String, statusText: String, extraText: String, isOverdue: Boolean = false) {
    val error = MaterialTheme.colorScheme.error
    AppCard(borderColor = if (isOverdue) error else CardBorder) {
        BookRow(title = title, subtitle = subtitle) {
            if (isOverdue) {
                StatusTag(statusText, dotColor = error, backgroundColor = MaterialTheme.colorScheme.errorContainer, textColor = error)
            } else {
                StatusTag(statusText, dotColor = BrandBlue, backgroundColor = StatusBlueBg, textColor = BrandBlue)
            }
            Text(extraText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Someone wants to borrow your book: amber "Pending" chip and Reject / Accept buttons.
@Composable
private fun RequestCard(title: String, subtitle: String, time: String) {
    AppCard {
        BookRow(title = title, subtitle = subtitle) {
            StatusTag(stringResource(R.string.status_pending), dotColor = StatusAmber, backgroundColor = StatusAmberBg, textColor = StatusAmber)
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        ) {
            OutlinedButton(onClick = { }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_reject))
            }
            Button(onClick = { }, shape = ButtonShape, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_accept))
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
