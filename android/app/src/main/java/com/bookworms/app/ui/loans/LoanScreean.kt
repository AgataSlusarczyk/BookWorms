// LoansScreen.kt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookworms.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Pożyczam", "U innych", "Prośby 2")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wypożyczenia", fontWeight = FontWeight.Bold) })
        },
        bottomBar = { AppBottomBar("Wypożyczenia") }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex, containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SectionHeader("Mamy u siebie", "2") }
                item {
                    LoanCard(
                        title = "Problem trzech ciał", subtitle = "od Marka W. • od 12.11",
                        statusText = "Zwrot do 20.11", statusDotColor = Color.Blue, extraText = "za 6 dni"
                    )
                }
                item {
                    LoanCard(
                        title = "Pachnidło", subtitle = "od Oli N. • od 03.10",
                        statusText = "Po terminie", statusDotColor = Color.Red, extraText = "4 dni zwłoki",
                        isOverdue = true
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
                item { SectionHeader("Czeka na Twoje potwierdzenie", "2") }
                
                item {
                    ActionCard(
                        title = "Solaris", subtitle = "Krzysiek P. prosi o wypożyczenie",
                        statusText = "Oczekujące", statusDotColor = Color(0xFFE6B800), extraText = "2 h temu",
                        primaryAction = "Akceptuj", secondaryAction = "Odrzuć"
                    )
                }
                item {
                    ActionCard(
                        title = "Lalka", subtitle = "Ola M. potwierdza zwrot",
                        statusText = "Oczekuje na zwrot", statusDotColor = Color(0xFFE6B800),
                        primaryAction = "Potwierdź zwrot"
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, count: String) {
    Text(
        text = "$title  $count",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun LoanCard(
    title: String, subtitle: String, statusText: String, statusDotColor: Color, 
    extraText: String, isOverdue: Boolean = false
) {
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
fun ActionCard(
    title: String, subtitle: String, statusText: String, statusDotColor: Color,
    extraText: String = "", primaryAction: String, secondaryAction: String? = null
) {
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