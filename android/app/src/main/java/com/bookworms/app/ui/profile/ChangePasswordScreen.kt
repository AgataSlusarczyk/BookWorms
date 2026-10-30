        package com.bookworms.app.ui.profile

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.auth.FieldShape
import com.bookworms.app.ui.auth.LabeledField
import com.bookworms.app.ui.auth.PasswordVisibilityButton
import com.bookworms.app.ui.auth.authFieldColors
import com.bookworms.app.ui.components.AppTopBar
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

private const val MinPasswordLength = 10

// The same rules as on the sign-up screen: at least 10 characters, a digit and a special character.
private fun isStrongPassword(password: String) =
    password.length >= MinPasswordLength &&
        password.any(Char::isDigit) &&
        password.any { !it.isLetterOrDigit() }

@Composable
fun ChangePasswordScreen(
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    // rememberSaveable, so typed text survives rotating the phone.
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var repeatPassword by rememberSaveable { mutableStateOf("") }

    // Errors show up only after something is typed, so an empty form isn't all red.
    val newPasswordError = when {
        newPassword.isEmpty() -> null
        !isStrongPassword(newPassword) -> R.string.error_password_too_weak
        newPassword == currentPassword -> R.string.error_password_same_as_current
        else -> null
    }
    val repeatPasswordError =
        if (repeatPassword.isNotEmpty() && repeatPassword != newPassword) R.string.error_passwords_dont_match else null

    val canSave = currentPassword.isNotEmpty() &&
        newPassword.isNotEmpty() &&
        newPasswordError == null &&
        repeatPassword == newPassword

    Scaffold(
        // Transparent, so the misted picture behind the whole screen shows through.
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = { AppTopBar(title = stringResource(R.string.change_password_title), onBackClick = onBackClick) },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                // imePadding makes room for the keyboard, so the field you type in moves up above it.
                .imePadding()
                // Scrolls on small phones and when the keyboard is open.
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.change_password_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            PasswordField(
                label = stringResource(R.string.current_password_label),
                value = currentPassword,
                onValueChange = { currentPassword = it },
            )
            PasswordField(
                label = stringResource(R.string.new_password_label),
                value = newPassword,
                onValueChange = { newPassword = it },
                // The rules stay visible under the field; when they're broken the text turns red.
                supportingText = stringResource(newPasswordError ?: R.string.password_hint),
                isError = newPasswordError != null,
            )
            PasswordField(
                label = stringResource(R.string.repeat_new_password_label),
                value = repeatPassword,
                onValueChange = { repeatPassword = it },
                supportingText = repeatPasswordError?.let { stringResource(it) },
                isError = repeatPasswordError != null,
                imeAction = ImeAction.Done,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSaveClick,
                enabled = canSave,
                shape = FieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.change_password_button))
            }
        }
    }
}

// Label above, password field below with the eye icon – the same as on the sign-up screen.
@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    // Each field remembers on its own whether the password is shown.
    var visible by rememberSaveable { mutableStateOf(false) }

    LabeledField(label = label) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            shape = FieldShape,
            colors = authFieldColors(),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { PasswordVisibilityButton(visible = visible, onToggle = { visible = !visible }) },
            supportingText = supportingText?.let { { Text(it) } },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun ChangePasswordScreenPreview() {
    BookWormsTheme {
        ChangePasswordScreen(onSaveClick = {}, onBackClick = {})
    }
}
