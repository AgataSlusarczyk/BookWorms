package com.bookworms.app.ui.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.R
import com.bookworms.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf(R.string.tab_borrowing, R.string.tab_lent, R.string.tab_requests)

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.loans_title), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            },
            bottomBar = { AppBottomBar("loans") }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTabIndex, 
                    containerColor = Color.Transparent
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
                    item { SectionHeader(stringResource(R.string.section_we_have), "2") }
                    
                    item {
                        LoanCard(
                            title = "Problem trzech ciał", 
                            subtitle = "od Marka W. • od 12.11",
                            statusText = stringResource(R.string.status_return_by) + " 20.11", 
                            statusDotColor = Color.Blue, 
                            extraText = "za 6 dni"
                        )
                    }
                    item {
                        LoanCard(
                            title = "Pachnidło", 
                            subtitle = "od Oli N. • od 03.10",
                            statusText = stringResource(R.string.status_overdue), 
                            statusDotColor = Color.Red, 
                            extraText = "4 dni zwłoki",
                            isOverdue = true
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { SectionHeader(stringResource(R.string.section_pending_confirmation), "2") }
                    
                    item {
                        ActionCard(
                            title = "Solaris", 
                            subtitle = "Krzysiek P. prosi o wypożyczenie",
                            statusText = stringResource(R.string.status_pending), 
                            statusDotColor = Color(0xFFE6B800), 
                            extraText = "2 h temu",
                            primaryAction = stringResource(R.string.action_accept), 
                            secondaryAction = stringResource(R.string.action_reject)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: String) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(4.dp)) {
        Text(
            text = "$title$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LoanCard(title: String, subtitle: String, statusText: String, statusDotColor: Color, extraText: String, isOverdue: Boolean = false) {
    val borderColor = if (isOverdue) Color.Red else MaterialTheme.colorScheme.outlineVariant
    val tagBg = if (isOverdue) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
    val tagText = if (isOverdue) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant

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
                        StatusTag(statusText, statusDotColor, Color(0xFFFFF8E1), Color(0xFFB8860B))
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