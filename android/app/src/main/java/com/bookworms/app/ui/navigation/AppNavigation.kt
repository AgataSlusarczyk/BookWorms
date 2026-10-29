package com.bookworms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bookworms.app.ui.auth.ForgotPasswordScreen
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks

// The names of the screens the app can go to.
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    const val FORGOT_PASSWORD = "forgot_password"
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
                onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onBrowseWithoutAccountClick = { navController.navigate(Routes.BROWSE) },
            )
        }

        composable(Routes.BROWSE) {
            var query by rememberSaveable { mutableStateOf("") }
            // Filters again only when the query changes, not on every redraw of the screen.
            val results = remember(query) { filterBooks(sampleBooks, query) }

            BrowseScreen(
                query = query,
                results = results,
                onQueryChange = { query = it },
                // Login is already under this screen, so we just go back to it.
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
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

        composable(Routes.FORGOT_PASSWORD) {
            // Temporary state – moves to a ViewModel when we add the real password reset.
            var email by rememberSaveable { mutableStateOf("") }
            var linkSent by rememberSaveable { mutableStateOf(false) }

            ForgotPasswordScreen(
                email = email,
                onEmailChange = { email = it },
                linkSent = linkSent,
                // TODO: ask the server to send the email once it can do that.
                onSendLinkClick = { linkSent = true },
                onUseDifferentEmailClick = { linkSent = false },
                // Login is already under this screen, so we just go back to it.
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}
