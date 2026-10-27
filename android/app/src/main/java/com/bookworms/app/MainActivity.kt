package com.bookworms.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.theme.BookWormsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookWormsTheme {
                // Temporary state – moves to LoginViewModel in the next step.
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                LoginScreen(
                    email = email,
                    password = password,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onLoginClick = {},
                    onForgotPasswordClick = {},
                    onRegisterClick = {},
                    onBrowseWithoutAccountClick = {},
                )
            }
        }
    }
}
