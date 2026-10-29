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

// Importy widoków autoryzacji i przeglądania
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks

// Importy nowych widoków aplikacji (upewnij się, że ścieżki pakietów się zgadzają)
import com.bookworms.app.ui.library.LibraryScreen
import com.bookworms.app.ui.library.MyBooksScreen
import com.bookworms.app.ui.library.AddBookManualScreen
import com.bookworms.app.ui.lists.MyListsScreen
import com.bookworms.app.ui.loans.LoansScreen
import com.bookworms.app.ui.notifications.NotificationsScreen
import com.bookworms.app.ui.profile.ProfileScreen

// Rozszerzona lista ścieżek
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    
    // Nowe ścieżki głównej aplikacji
    const val LIBRARY = "library"
    const val LOANS = "loans"
    const val LISTS = "lists"
    const val PROFILE = "profile"
    
    // Ścieżki podrzędne
    const val NOTIFICATIONS = "notifications"
    const val MY_BOOKS = "my_books"
    const val ADD_BOOK_MANUAL = "add_book_manual"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Pomocnicza funkcja do nawigacji dolnym paskiem - zapobiega budowaniu nieskończonego stosu ekranów
    val navigateBottomBar: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.LIBRARY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        
        // --- SEKCJA LOGOWANIA I REJESTRACJI ---

        composable(Routes.LOGIN) {
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }

            LoginScreen(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onLoginClick = { 
                    // Po zalogowaniu przechodzimy do biblioteki i usuwamy ekran logowania z historii
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onForgotPasswordClick = {},
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onBrowseWithoutAccountClick = { navController.navigate(Routes.BROWSE) },
            )
        }

        composable(Routes.BROWSE) {
            var query by rememberSaveable { mutableStateOf("") }
            val results = remember(query) { filterBooks(sampleBooks, query) }

            BrowseScreen(
                query = query,
                results = results,
                onQueryChange = { query = it },
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.REGISTER) {
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
                onCreateAccountClick = {
                    // Po utworzeniu konta również przechodzimy od razu do aplikacji
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        // --- GŁÓWNA APLIKACJA (DOLNY PASEK NAWIGACJI) ---

        composable(Routes.LIBRARY) {
            LibraryScreen(onNavigate = navigateBottomBar)
        }

        composable(Routes.LOANS) {
            LoansScreen(onNavigate = navigateBottomBar)
        }

        composable(Routes.LISTS) {
            MyListsScreen(onNavigate = navigateBottomBar)
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigate = navigateBottomBar,
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToMyBooks = { navController.navigate(Routes.MY_BOOKS) },
                onLogoutClick = {
                    // Wylogowanie - powrót do logowania i całkowite wyczyszczenie stosu ekranów
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- POD-EKRANY (BEZ DOLNEGO PASKA) ---

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.MY_BOOKS) {
            MyBooksScreen(
                onBackClick = { navController.popBackStack() },
                onAddBookClick = { navController.navigate(Routes.ADD_BOOK_MANUAL) }
            )
        }

        composable(Routes.ADD_BOOK_MANUAL) {
            AddBookManualScreen(onBackClick = { navController.popBackStack() })
        }
    }
}