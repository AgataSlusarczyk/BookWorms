package com.bookworms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen

// The names of the screens the app can go to.
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

@Composable
fun AppNavigation() {
    // Remembers which screen is open and which ones are behind it.
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            // Temporary state – moves to a ViewModel when we add the real login.
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }

            LoginScreen(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onLoginClick = {},
                onForgotPasswordClick = {},
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onBrowseWithoutAccountClick = {},
            )
        }

        composable(Routes.REGISTER) {
            // Temporary state – moves to a ViewModel when we add the real sign-up.
            var name by rememberSaveable { mutableStateOf("") }
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }
            var repeatPassword by rememberSaveable { mutableStateOf("") }
            var termsAccepted by rememberSaveable { mutableStateOf(false) }

            RegisterScreen(
                name = name,
                email = email,
                password = password,
                repeatPassword = repeatPassword,
                onNameChange = { name = it },
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onRepeatPasswordChange = { repeatPassword = it },
                termsAccepted = termsAccepted,
                onTermsAcceptedChange = { termsAccepted = it },
                onCreateAccountClick = {},
                // Login is already under this screen, so we just go back to it.
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
