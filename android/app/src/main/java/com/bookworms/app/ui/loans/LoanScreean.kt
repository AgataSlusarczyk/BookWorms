package com.bookworms.app.ui.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.components.*
import com.bookworms.app.ui.theme.BookWormsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(R.string.tab_borrowing, R.string.tab_lent, R.string.tab_requests)

    Scaffold(
        modifier = Modifier, 
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loans_title), fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { AppBottomBar("loans") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
            ) {
                tabs.forEachIndexed { index, titleResId ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(stringResource(titleResId)) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SectionHeader(stringResource(R.string.section_we_have, "2")) }
                
                item {
                    LoanCard(
                        title = "Problem trzech ciał", 
                        subtitle = stringResource(R.string.loan_subtitle, "Marek W.", "12.11"),
                        statusText = stringResource(R.string.status_return_by, "20.11"), 
                        statusDotColor = MaterialTheme.colorScheme.primary, 
                        extraText = pluralStringResource(R.plurals.in_x_days, 6, 6)
                    )
                }
                item {
                    LoanCard(
                        title = "Pachnidło", 
                        subtitle = stringResource(R.string.loan_subtitle, "Ola N.", "03.10"),
                        statusText = stringResource(R.string.status_overdue), 
                        statusDotColor = MaterialTheme.colorScheme.error, 
                        extraText = pluralStringResource(R.plurals.x_days_overdue, 4, 4),
                        isOverdue = true
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader(stringResource(R.string.section_pending_confirmation, "2")) }
                
                item {
                    ActionCard(
                        title = "Solaris", 
                        subtitle = stringResource(R.string.asks_to_borrow, "Krzysiek P."),
                        statusText = stringResource(R.string.status_pending), 
                        statusDotColor = MaterialTheme.colorScheme.tertiary, 
                        extraText = stringResource(R.string.x_hours_ago, "2"),
                        primaryAction = stringResource(R.string.action_accept), 
                        secondaryAction = stringResource(R.string.action_reject)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun LoanCard(title: String, subtitle: String, statusText: String, statusDotColor: Color, extraText: String, isOverdue: Boolean = false) {
    val borderColor = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val tagBg = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val tagText = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedCard(
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCoverPlaceholder()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusTag(statusText, statusDotColor, tagBg, tagText)
                    Text(extraText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, statusText: String, statusDotColor: Color, extraText: String = "", primaryAction: String, secondaryAction: String? = null) {
    OutlinedCard(
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                BookCoverPlaceholder()
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusTag(statusText, statusDotColor, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        if (extraText.isNotEmpty()) Text(extraText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (secondaryAction != null) {
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text(secondaryAction)
                    }
                }
                Button(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text(primaryAction)
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoansScreenPreview() {
    BookWormsTheme {
        LoansScreen()
    }
}