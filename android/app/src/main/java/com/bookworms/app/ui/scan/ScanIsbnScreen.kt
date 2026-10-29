package com.bookworms.app.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.components.CloseTitle
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun ScanIsbnScreen(
    // The code the camera read, or null while it's still looking for one.
    recognizedIsbn: String?,
    onEnterIsbnManuallyClick: () -> Unit,
    onAddWithoutIsbnClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        CloseTitle(title = stringResource(R.string.scan_isbn_title), onCloseClick = onCloseClick)

        Spacer(Modifier.height(16.dp))

        // weight(1f) = take the space that's left, so the buttons stay at the bottom.
        CameraPlaceholder(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        if (recognizedIsbn != null) {
            Spacer(Modifier.height(12.dp))
            RecognizedCode(recognizedIsbn)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onEnterIsbnManuallyClick,
            shape = FieldShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            // null, because the button text already says what it does.
            Icon(
                painter = painterResource(R.drawable.ic_barcode_scan),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.enter_isbn_manually))
        }

        TextButton(
            onClick = onAddWithoutIsbnClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.add_book_without_isbn))
        }
    }
}

// Dark box where the camera picture will be, with a frame to aim at the barcode.
// TODO: show the camera picture and read the barcode when we add a scanner.
@Composable
private fun CameraPlaceholder(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(
            // A bit lighter in the middle, like a camera picture in a dark room.
            brush = Brush.radialGradient(listOf(CameraGray, CameraBlack)),
            shape = RoundedCornerShape(16.dp),
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.73f)
                .aspectRatio(2f)
                .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp)),
        ) {
            // The red line that shows where the barcode should be.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp)
                    .background(ScanLineRed),
            )
        }

        Text(
            text = stringResource(R.string.scan_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

// Light blue box with the code the camera read.
@Composable
private fun RecognizedCode(isbn: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), FieldShape)
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.recognized_code),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = isbn,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        // TODO: get the book from the server and open its details.
        Text(
            text = stringResource(R.string.fetching_book_details),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val CameraBlack = Color(0xFF0C1115)
private val CameraGray = Color(0xFF262B30)
private val ScanLineRed = Color(0xFFC03B36)

@Preview(device = "id:pixel_8")
@Composable
private fun ScanIsbnScreenPreview() {
    BookWormsTheme {
        ScanIsbnScreen(
            recognizedIsbn = "978-83-08-04815-3",
            onEnterIsbnManuallyClick = {},
            onAddWithoutIsbnClick = {},
            onCloseClick = {},
        )
    }
}
