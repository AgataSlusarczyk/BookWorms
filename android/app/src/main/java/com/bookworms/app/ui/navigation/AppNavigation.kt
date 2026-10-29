package com.bookworms.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.book.BookDetailsScreen
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.book.sampleBookDetailsFor
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.group.InviteToGroupScreen
import com.bookworms.app.ui.group.copyInviteCode
import com.bookworms.app.ui.group.sampleInvite
import com.bookworms.app.ui.group.sampleRecentMembers
import com.bookworms.app.ui.group.shareInviteLink
import com.bookworms.app.ui.scan.ScanIsbnScreen

// The names of the screens the app can go to.
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    // {isbn} is filled in with the book to show, e.g. "book_details/9788308049461".
    const val BOOK_DETAILS = "book_details/{isbn}"
    const val SCAN_ISBN = "scan_isbn"
    const val INVITE_TO_GROUP = "invite_to_group"

    fun bookDetails(isbn: String) = "book_details/$isbn"
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
                onCreateAccountClick = {},
                // Login is already under this screen, so we just go back to it.
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
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

        // Nothing opens this screen yet – it gets linked when groups are added.
        composable(Routes.INVITE_TO_GROUP) {
            // Needed for the clipboard and the share menu.
            val context = LocalContext.current

            InviteToGroupScreen(
                invite = sampleInvite,
                recentMembers = sampleRecentMembers,
                onCopyCodeClick = { copyInviteCode(context, sampleInvite.code) },
                onShareLinkClick = { shareInviteLink(context, sampleInvite) },
                onGenerateNewCodeClick = {},
                onCloseClick = { navController.popBackStack() },
            )
        }
    }
}
