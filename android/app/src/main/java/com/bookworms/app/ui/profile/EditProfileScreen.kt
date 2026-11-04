package com.bookworms.app.ui.profile

import android.net.Uri
import android.util.Patterns
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.bookworms.app.ui.auth.UsernamePattern
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.ProfilePhoto
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun EditProfileScreen(
    // photo = the picked photo, or the old one when the user didn't pick a new one.
    onSaveClick: (photo: Uri?) -> Unit,
    onBackClick: () -> Unit,
    photoUri: Uri? = null,
) {
    // The new photo is only a draft until Save, so Back leaves the old one.
    var photo by rememberSaveable { mutableStateOf(photoUri) }
    // The phone's own photo picker. It needs no permission, because the user picks the photo themselves.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { picked ->
        // null = the user closed the picker without choosing anything.
        if (picked != null) photo = picked
    }
    // TODO: start with the user's data from the server. rememberSaveable keeps it when the phone rotates.
    var name by rememberSaveable { mutableStateOf("Anna Kowalska") }
    var username by rememberSaveable { mutableStateOf("anna_k") }
    var email by rememberSaveable { mutableStateOf("anna.kowalska@mail.com") }

    // Spaces at the ends are easy to type by accident, so they don't count.
    val usernameValid = UsernamePattern.matches(username.trim())
    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

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
            ProfilePhoto(uri = photo, size = 96.dp)
            TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Text(stringResource(R.string.change_photo))
            }

            Spacer(Modifier.height(8.dp))

            ProfileField(
                label = stringResource(R.string.name_label),
                value = name,
                onValueChange = { name = it },
            )
            ProfileField(
                label = stringResource(R.string.username_label),
                value = username,
                // People often type "@anna" – the "@" is already shown in front, so we drop it.
                onValueChange = { username = it.removePrefix("@") },
                prefix = "@",
                supportingText = stringResource(
                    if (username.isNotEmpty() && !usernameValid) R.string.error_username_invalid else R.string.username_hint,
                ),
                isError = username.isNotEmpty() && !usernameValid,
                keyboardType = KeyboardType.Ascii,
            )
            ProfileField(
                label = stringResource(R.string.email_label),
                value = email,
                onValueChange = { email = it },
                supportingText = if (email.isNotEmpty() && !emailValid) stringResource(R.string.error_email_invalid) else null,
                isError = email.isNotEmpty() && !emailValid,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onSaveClick(photo) },
                // Spaces alone don't count as a name.
                enabled = name.isNotBlank() && usernameValid && emailValid,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.save_action))
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
        EditProfileScreen(onSaveClick = {}, onBackClick = {})
    }
}
