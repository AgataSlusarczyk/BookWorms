package com.bookworms.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.data.LoadState

// Spinning circle while data comes from the server.
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        CircularProgressIndicator()
    }
}

// A whole screen that waits for its data: a back arrow with a spinning circle or "Try again"
// until the data is there, then content() draws the real screen.
@Composable
fun <T> LoadStateScreen(
    state: LoadState<T>,
    failedMessage: String,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    if (state is LoadState.Ready) {
        content(state.data)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        BackTitle(title = "", onBackClick = onBackClick)
        when (state) {
            LoadState.Loading -> LoadingIndicator()
            LoadState.Failed -> LoadFailedMessage(failedMessage, onRetryClick)
            is LoadState.Ready -> Unit
        }
    }
}

// Shown when the server couldn't be reached, with a button to ask again.
@Composable
fun LoadFailedMessage(message: String, onRetryClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetryClick) {
            Text(stringResource(R.string.try_again))
        }
    }
}
