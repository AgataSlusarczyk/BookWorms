package com.bookworms.app.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun EditProfileScreen(
    name: String,
    username: String,
    email: String,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    // Errors under the fields, null = the field is fine.
    usernameError: String?,
    emailError: String?,
    // An error from the server above the button, null = none.
    errorMessage: String?,
    saving: Boolean,
    saveEnabled: Boolean,
    // newPhoto = the photo picked on this screen, or null when the user didn't pick one.
    onSaveClick: (newPhoto: Uri?) -> Unit,
    onBackClick: () -> Unit,
    // Full address of the current photo on the server, or null when there's none.
    photoUrl: String? = null,
) {
    // The new photo is only a draft until Save, so Back leaves the old one.
    var newPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    // The phone's own photo picker. It needs no permission, because the user picks the photo themselves.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        // null = the user closed the picker without choosing anything.
        if (picked != null) newPhoto = picked
    }
    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.edit_profile_title), onBackClick = onBackClick) },
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // The same circle as in Profile, only bigger.
            ProfilePhoto(photo = newPhoto ?: photoUrl, size = 96.dp)
            TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(stringResource(R.string.change_photo))
            }

            Spacer(Modifier.height(8.dp))

            ProfileField(
                label = stringResource(R.string.name_label),
                value = name,
                onValueChange = onNameChange,
            )
            ProfileField(
                label = stringResource(R.string.username_label),
                value = username,
                onValueChange = onUsernameChange,
                prefix = "@",
                supportingText = usernameError ?: stringResource(R.string.username_hint),
                isError = usernameError != null,
                keyboardType = KeyboardType.Ascii,
            )
            ProfileField(
                label = stringResource(R.string.email_label),
                value = email,
                onValueChange = onEmailChange,
                supportingText = emailError,
                isError = emailError != null,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onSaveClick(newPhoto) },
                // Only once every field is filled in correctly.
                enabled = saveEnabled,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.save_action))
                }
            }
        }
    }
}

// Label above, white see-through field below – the same as on the other forms.
@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    LabeledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            prefix = prefix?.let { { Text(it) } },
            supportingText = supportingText?.let { { Text(it) } },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun EditProfileScreenPreview() {
    BookWormsTheme {
        EditProfileScreen(
            name = "Anna Kowalska",
            username = "anna_k",
            email = "anna.kowalska@mail.com",
            onNameChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            usernameError = null,
            emailError = null,
            errorMessage = null,
            saving = false,
            saveEnabled = true,
            onSaveClick = {},
            onBackClick = {},
        )
    }
}
