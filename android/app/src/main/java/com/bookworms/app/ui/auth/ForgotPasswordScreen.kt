package com.bookworms.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookworms.app.R
import com.bookworms.app.ui.components.AppHeader
import com.bookworms.app.ui.components.BackTitle
import com.bookworms.app.ui.components.mistedBackground
import com.bookworms.app.ui.theme.BookWormsTheme

@Composable
fun ForgotPasswordScreen(
    email: String,
    onEmailChange: (String) -> Unit,
    // true after "Send reset link", then the screen shows "Check your inbox" instead of the form.
    linkSent: Boolean,
    onSendLinkClick: () -> Unit,
    onUseDifferentEmailClick: () -> Unit,
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .mistedBackground()
            .safeDrawingPadding()
            // Scrolls on small phones when the keyboard is open.
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
    ) {
        AppHeader()

        Spacer(Modifier.height(16.dp))

        BackTitle(title = stringResource(R.string.reset_password_title), onBackClick = onBackClick)

        Spacer(Modifier.height(8.dp))

        if (linkSent) {
            LinkSentMessage(
                email = email,
                onLoginClick = onLoginClick,
                onUseDifferentEmailClick = onUseDifferentEmailClick,
            )
        } else {
            ResetForm(
                email = email,
                onEmailChange = onEmailChange,
                onSendLinkClick = onSendLinkClick,
                onLoginClick = onLoginClick,
            )
        }
    }
}

@Composable
private fun ResetForm(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendLinkClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    // There's nothing to send to until the user types something.
    val canSend = email.isNotBlank()

    Column {
        Text(
            text = stringResource(R.string.reset_password_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(24.dp))

        LabeledField(label = stringResource(R.string.email_label)) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                singleLine = true,
                shape = FieldShape,
                colors = authFieldColors(),
                // The Enter key on the keyboard sends the link right away.
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSendLinkClick() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSendLinkClick,
            enabled = canSend,
            shape = FieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.send_reset_link))
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.remember_password),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onLoginClick) {
                Text(stringResource(R.string.login_button))
            }
        }
    }
}

// Shown after the link is sent: where it went and what to do next.
@Composable
private fun LinkSentMessage(
    email: String,
    onLoginClick: () -> Unit,
    onUseDifferentEmailClick: () -> Unit,
) {
    Column {
        // Light blue box, same as the guest notice on the browse screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), FieldShape)
                .padding(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mail),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.check_inbox_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.reset_link_sent, email.trim()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
            shape = FieldShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.back_to_login))
        }

        TextButton(
            onClick = onUseDifferentEmailClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.use_different_email))
        }
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun ForgotPasswordScreenPreview() {
    BookWormsTheme {
        ForgotPasswordScreen(
            email = "anna.kowalska@mail.com",
            onEmailChange = {},
            linkSent = false,
            onSendLinkClick = {},
            onUseDifferentEmailClick = {},
            onLoginClick = {},
            onBackClick = {},
        )
    }
}

@Preview(device = "id:pixel_8")
@Composable
private fun ForgotPasswordLinkSentPreview() {
    BookWormsTheme {
        ForgotPasswordScreen(
            email = "anna.kowalska@mail.com",
            onEmailChange = {},
            linkSent = true,
            onSendLinkClick = {},
            onUseDifferentEmailClick = {},
            onLoginClick = {},
            onBackClick = {},
        )
    }
}
