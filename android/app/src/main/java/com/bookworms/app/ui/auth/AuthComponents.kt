package com.bookworms.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookworms.app.R

// Pieces shared by the login and register screens.

internal val FieldShape = RoundedCornerShape(8.dp)

// 3–20 letters, digits, dots or underscores, e.g. "ola" or "marek_w".
internal val UsernamePattern = Regex("[A-Za-z0-9._]{3,20}")

@Composable
internal fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        field()
    }
}

// White, a bit see-through, so the background shows through the fields slightly.
private val FieldFill = Color.White.copy(alpha = 0.8f)

// Colors for all text fields on the login and register screens.
@Composable
internal fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FieldFill,
    unfocusedContainerColor = FieldFill,
)

// The eye icon at the end of a password field.
@Composable
internal fun PasswordVisibilityButton(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
            contentDescription = stringResource(if (visible) R.string.hide_password else R.string.show_password),
        )
    }
}
