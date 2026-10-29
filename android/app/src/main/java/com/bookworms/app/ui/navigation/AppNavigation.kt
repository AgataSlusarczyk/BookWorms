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
import com.bookworms.app.ui.book.BookDetailsScreen
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.book.sampleBookDetailsFor
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.library.AddBookManualScreen
import com.bookworms.app.ui.library.LibraryScreen
import com.bookworms.app.ui.library.MyBooksScreen
import com.bookworms.app.ui.lists.MyListsScreen
import com.bookworms.app.ui.loans.LoansScreen
import com.bookworms.app.ui.notifications.NotificationsScreen
import com.bookworms.app.ui.profile.ProfileScreen
import com.bookworms.app.ui.scan.ScanIsbnScreen

// The names of the screens the app can go to.
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    const val FORGOT_PASSWORD = "forgot_password"
    // {isbn} is filled in with the book to show, e.g. "book_details/9788308049461".
    const val BOOK_DETAILS = "book_details/{isbn}"
    const val SCAN_ISBN = "scan_isbn"

    // Tabs of the bottom bar – the same names AppBottomBar sends in onNavigate.
    const val LIBRARY = "library"
    const val LOANS = "loans"
    const val LISTS = "lists"
    const val PROFILE = "profile"

    // Screens opened from a tab, without the bottom bar.
    const val NOTIFICATIONS = "notifications"
    const val MY_BOOKS = "my_books"
    const val ADD_BOOK_MANUAL = "add_book_manual"

    fun bookDetails(isbn: String) = "book_details/$isbn"
}

@Composable
fun AppNavigation() {
    // Remembers which screen is open and which ones are behind it.
    val navController = rememberNavController()

    // Switching tabs in the bottom bar: no growing pile of screens, and each tab keeps
    // its state (scroll position, selected tab) when you come back to it.
    val navigateBottomBar: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.LIBRARY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                onLoginClick = {
                    navController.navigate(Routes.LIBRARY) {
                        // Removes login from the pile, so "back" doesn't return to it.
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
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
                onBookClick = { book -> navController.navigate(Routes.bookDetails(book.isbn)) },
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
                // TODO: create the account on the server first. For now it goes straight into the app.
                onCreateAccountClick = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
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
                    navController.navigate(Routes.LOGIN) {
                        // Clears every screen, so after logging out "back" can't open the app's inside.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.MY_BOOKS) {
            MyBooksScreen(
                onBackClick = { navController.popBackStack() },
                onAddBookClick = { navController.navigate(Routes.ADD_BOOK_MANUAL) },
            )
        }

        composable(Routes.ADD_BOOK_MANUAL) {
            AddBookManualScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.BOOK_DETAILS) { entry ->
            val isbn = entry.arguments?.getString("isbn")
            // Temporary – looks the book up in the sample list until books come from the server.
            val book = remember(isbn) { sampleBooks.first { it.isbn == isbn } }

            // Temporary state – moves to a ViewModel when book details come from the server.
            var isFavorite by rememberSaveable { mutableStateOf(false) }
            var readingStatus by rememberSaveable { mutableStateOf(ReadingStatus.TO_READ) }

            BookDetailsScreen(
                book = sampleBookDetailsFor(book),
                isFavorite = isFavorite,
                readingStatus = readingStatus,
                onFavoriteClick = { isFavorite = !isFavorite },
                onMoreClick = {},
                onReadingStatusChange = { readingStatus = it },
                onRequestLoanClick = {},
                onJoinQueueClick = {},
                onAddCommentClick = {},
                onBackClick = { navController.popBackStack() },
            )
        }

        // Nothing opens this screen yet – it gets linked when the library is added.
        composable(Routes.SCAN_ISBN) {
            ScanIsbnScreen(
                // Temporary – a sample code until the camera can read barcodes.
                recognizedIsbn = "978-83-08-04815-3",
                onEnterIsbnManuallyClick = {},
                onAddWithoutIsbnClick = {},
                onCloseClick = { navController.popBackStack() },
            )
        }
    }
}
