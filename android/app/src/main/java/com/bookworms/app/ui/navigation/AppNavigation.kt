package com.bookworms.app.ui.navigation

import android.net.Uri
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
import com.bookworms.app.ui.auth.ForgotPasswordScreen
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.book.BookDetailsScreen
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.book.sampleBookDetailsFor
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.groups.CreateGroupScreen
import com.bookworms.app.ui.groups.GroupDetailsScreen
import com.bookworms.app.ui.groups.GroupVisibility
import com.bookworms.app.ui.groups.GroupsScreen
import com.bookworms.app.ui.groups.JoinGroupScreen
import com.bookworms.app.ui.group.InviteToGroupScreen
import com.bookworms.app.ui.group.copyInviteCode
import com.bookworms.app.ui.group.sampleInvite
import com.bookworms.app.ui.group.sampleRecentMembers
import com.bookworms.app.ui.group.shareInviteLink
import com.bookworms.app.ui.library.AddBookManualScreen
import com.bookworms.app.ui.library.LibraryScreen
import com.bookworms.app.ui.library.MyBooksScreen
import com.bookworms.app.ui.lists.CreateListScreen
import com.bookworms.app.ui.lists.MyListDetailsScreen
import com.bookworms.app.ui.lists.MyListsScreen
import com.bookworms.app.ui.loans.LoansScreen
import com.bookworms.app.ui.notifications.NotificationsScreen
import com.bookworms.app.ui.profile.ChangePasswordScreen
import com.bookworms.app.ui.profile.EditProfileScreen
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
    const val INVITE_TO_GROUP = "invite_to_group"

    // Tabs of the bottom bar – the same names AppBottomBar sends in onNavigate.
    const val LIBRARY = "library"
    const val LOANS = "loans"
    const val LISTS = "lists"
    const val GROUPS = "groups"
    const val PROFILE = "profile"

    // Screens opened from a tab, without the bottom bar.
    const val NOTIFICATIONS = "notifications"
    const val MY_BOOKS = "my_books"
    const val ADD_BOOK_MANUAL = "add_book_manual"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val CHANGE_PASSWORD = "change_password"
    const val EDIT_PROFILE = "edit_profile"
    const val CREATE_LIST = "create_list"
    const val LIST_DETAILS = "list_details/{listName}"
    const val GROUP_DETAILS = "group_details/{groupName}"

    fun bookDetails(isbn: String) = "book_details/$isbn"

    // Uri.encode turns spaces and other special characters into a form that fits in a route.
    fun listDetails(listName: String) = "list_details/${Uri.encode(listName)}"
    fun groupDetails(groupName: String) = "group_details/${Uri.encode(groupName)}"
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

        composable(Routes.GROUPS) {
            GroupsScreen(
                onNavigate = navigateBottomBar,
                onCreateGroupClick = { navController.navigate(Routes.CREATE_GROUP) },
                onJoinGroupClick = { navController.navigate(Routes.JOIN_GROUP) },
                onGroupClick = { groupName -> navController.navigate(Routes.groupDetails(groupName)) },
            )
        }

        composable(Routes.LISTS) {
            MyListsScreen(
                onNavigate = navigateBottomBar,
                onListClick = { listName -> navController.navigate(Routes.listDetails(listName)) },
                onCreateListClick = { navController.navigate(Routes.CREATE_LIST) },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigate = navigateBottomBar,
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToMyBooks = { navController.navigate(Routes.MY_BOOKS) },
                onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                onChangePasswordClick = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onLogoutClick = {
                    navController.navigate(Routes.LOGIN) {
                        // Clears every screen, so after logging out "back" can't open the app's inside.
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CREATE_LIST) {
            CreateListScreen(
                // Opens the new list. popUpTo takes Create list off the stack, so Back goes to My lists, not here.
                onCreateClick = { listName ->
                    navController.navigate(Routes.listDetails(listName)) {
                        popUpTo(Routes.CREATE_LIST) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.LIST_DETAILS) { backStackEntry ->
            // The route gives the name back already decoded, e.g. "Holiday reads".
            val listName = backStackEntry.arguments?.getString("listName").orEmpty()
            MyListDetailsScreen(listName = listName, onBackClick = { navController.popBackStack() })
        }

        composable(Routes.GROUP_DETAILS) { backStackEntry ->
            // TODO: look the group up on the server. For now only the name comes from the list.
            val groupName = backStackEntry.arguments?.getString("groupName").orEmpty()
            GroupDetailsScreen(
                groupName = groupName,
                // Temporary – in the sample data Anna is the admin of "Czytelnia 23" only.
                isAdmin = groupName == "Czytelnia 23",
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                // TODO: open the group chat when that screen exists.
                onChatClick = {},
                onInviteClick = { navController.navigate(Routes.INVITE_TO_GROUP) },
                // TODO: leave the group on the server first. For now it just goes back to Groups.
                onLeaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                // TODO: save the name on the server first. For now it just goes back to Profile.
                onSaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(
                // TODO: send the passwords to the server first. For now it just goes back to Profile.
                onSaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
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
            AddBookManualScreen(
                onScanIsbnClick = { navController.navigate(Routes.SCAN_ISBN) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.JOIN_GROUP) {
            JoinGroupScreen(
                // Opens the new group. popUpTo takes Join group off the stack, so Back goes to Groups, not here.
                onGroupJoined = { groupName ->
                    navController.navigate(Routes.groupDetails(groupName)) {
                        popUpTo(Routes.JOIN_GROUP) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATE_GROUP) {
            // Temporary state – moves to a ViewModel when groups come from the server.
            var name by rememberSaveable { mutableStateOf("") }
            var description by rememberSaveable { mutableStateOf("") }
            var visibility by rememberSaveable { mutableStateOf(GroupVisibility.Private) }
            var invitedUsernames by rememberSaveable { mutableStateOf(listOf<String>()) }

            CreateGroupScreen(
                name = name,
                description = description,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                visibility = visibility,
                onVisibilityChange = { visibility = it },
                invitedUsernames = invitedUsernames,
                onAddInvite = { invitedUsernames = invitedUsernames + it },
                onRemoveInvite = { invitedUsernames = invitedUsernames - it },
                // TODO: create the group on the server first. For now it just goes back to Groups.
                onCreateGroupClick = { navController.popBackStack() },
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

        // Opened from Add book. Both buttons go back to the form, where the details can be typed in.
        // TODO: when the camera reads a code, send it back to the form and fill in the book details.
        composable(Routes.SCAN_ISBN) {
            ScanIsbnScreen(
                // Temporary – a sample code until the camera can read barcodes.
                recognizedIsbn = "978-83-08-04815-3",
                onEnterIsbnManuallyClick = { navController.popBackStack() },
                onAddWithoutIsbnClick = { navController.popBackStack() },
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
