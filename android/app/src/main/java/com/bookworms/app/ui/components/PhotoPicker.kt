package com.bookworms.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R

// The big circle on Edit profile and Edit group, with "Change photo" and "Remove photo" under it.
// Nothing is sent until Save: newPhoto and removed are only a draft the screen keeps, so Back leaves the old photo.
@Composable
fun PhotoPicker(
    // Full address of the photo on the server now, or null when there's none.
    currentUrl: String?,
    // A photo picked and cropped on this screen, or null.
    newPhoto: Uri?,
    // true = "Remove photo" was tapped.
    removed: Boolean,
    // Gets the cropped photo, not the one picked in the gallery.
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // The photo just picked in the gallery, waiting to be cropped. Saveable, so turning the phone keeps it.
    var toCrop by rememberSaveable { mutableStateOf<Uri?>(null) }
    // The phone's own photo picker. It needs no permission, because the user picks the photo themselves.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        // null = the user closed the picker without choosing anything.
        if (picked != null) toCrop = picked
    }
    val shown: Any? = newPhoto ?: currentUrl.takeUnless { removed }

    toCrop?.let { photo ->
        PhotoCropDialog(
            photo = photo,
            onDone = { cropped ->
                toCrop = null
                onPick(cropped)
            },
            // The photo shown before stays.
            onCancel = { toCrop = null },
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        ProfilePhoto(photo = shown, size = 96.dp)
        Row {
            TextButton(
                onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = enabled,
            ) {
                Text(stringResource(R.string.change_photo))
            }
            if (shown != null) {
                TextButton(
                    onClick = onRemove,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.remove_photo))
                }
            }
        }
    }
}
