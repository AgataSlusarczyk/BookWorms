package com.bookworms.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppHeader
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun RegisterScreen(
    name: String,
    email: String,
    password: String,
    repeatPassword: String,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRepeatPasswordChange: (String) -> Unit,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    onCreateAccountClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var repeatPasswordVisible by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            // The same picture as on the login screen...
            .paint(
                painter = painterResource(R.drawable.bg_login),
                contentScale = ContentScale.Crop,
                sizeToIntrinsics = false,
            )
            // ...covered with thick paper-colored mist, so only a hint of it shows through.
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.89f))
            .safeDrawingPadding()
            // The form is long, so it scrolls on small phones and when the keyboard is open.
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        AppHeader()

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBackClick,
                // The button is bigger than the arrow (easier to tap), so we move it left
                // to line the arrow up with the rest of the screen.
                modifier = Modifier.offset(x = (-12).dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.offset(x = (-8).dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        LabeledField(label = stringResource(R.string.name_label)) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        LabeledField(label = stringResource(R.string.email_label)) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        LabeledField(label = stringResource(R.string.password_label)) {
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityButton(
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible },
                    )
                },
                // Small gray text under the field.
                supportingText = { Text(stringResource(R.string.password_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))

        LabeledField(label = stringResource(R.string.repeat_password_label)) {
            OutlinedTextField(
                value = repeatPassword,
                onValueChange = onRepeatPasswordChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                visualTransformation = if (repeatPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityButton(
                        visible = repeatPasswordVisible,
                        onToggle = { repeatPasswordVisible = !repeatPasswordVisible },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        // The whole row is clickable, not just the small box, so it's easier to tap.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                // Lines the box up with the fields, same as the back arrow.
                .offset(x = (-12).dp)
                .toggleable(
                    value = termsAccepted,
                    onValueChange = onTermsAcceptedChange,
                    role = Role.Checkbox,
                ),
        ) {
            // null, because the row above already handles the click.
            Checkbox(checked = termsAccepted, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Text(
                text = stringResource(R.string.accept_terms),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onCreateAccountClick,
            // You can't create an account until you accept the terms.
            enabled = termsAccepted,
            shape = FieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.create_account_button))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.have_account),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onLoginClick) {
                Text(stringResource(R.string.login_button))
            }
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun RegisterScreenPreview() {
    BookWormsTheme {
        RegisterScreen(
            name = "Anna",
            email = "anna.kowalska@mail.com",
            password = "secret-password",
            repeatPassword = "",
            onNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onRepeatPasswordChange = {},
            termsAccepted = true,
            onTermsAcceptedChange = {},
            onCreateAccountClick = {},
            onLoginClick = {},
            onBackClick = {},
        )
    }
}
