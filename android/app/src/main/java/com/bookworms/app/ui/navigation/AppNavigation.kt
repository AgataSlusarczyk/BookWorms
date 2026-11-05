package com.bookworms.app.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookworms.app.R
import com.bookworms.app.ui.auth.ForgotPasswordScreen
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.book.AddCommentScreen
import com.bookworms.app.ui.book.BookDetailsScreen
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.book.findGroupComment
import com.bookworms.app.ui.book.sampleBookDetailsFor
import com.bookworms.app.ui.book.shareBook
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.filterBooks
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.profilePhotoUri
import com.bookworms.app.ui.groups.CreateGroupScreen
import com.bookworms.app.ui.groups.GroupDetailsScreen
import com.bookworms.app.ui.groups.GroupInfo
import com.bookworms.app.ui.groups.GroupVisibility
import com.bookworms.app.ui.groups.GroupsScreen
import com.bookworms.app.ui.groups.JoinGroupScreen
import com.bookworms.app.ui.groups.groupInfo
import com.bookworms.app.ui.groups.sampleGroups
import com.bookworms.app.ui.group.InviteToGroupScreen
import com.bookworms.app.ui.group.copyInviteCode
import com.bookworms.app.ui.group.newInviteCode
import com.bookworms.app.ui.group.withCode
import com.bookworms.app.ui.group.sampleInvite
import com.bookworms.app.ui.group.sampleRecentMembers
import com.bookworms.app.ui.group.shareInviteLink
import com.bookworms.app.ui.library.AddBookManualScreen
import com.bookworms.app.ui.library.LibraryScreen
import com.bookworms.app.ui.library.MyBooksScreen
import com.bookworms.app.ui.library.MyCopyScreen
import com.bookworms.app.ui.library.OwnBook
import com.bookworms.app.ui.library.myGroups
import com.bookworms.app.ui.library.ownBook
import com.bookworms.app.ui.library.sampleOwnBooks
import com.bookworms.app.ui.lists.AddBooksToListScreen
import com.bookworms.app.ui.lists.CreateListScreen
import com.bookworms.app.ui.lists.EditListScreen
import com.bookworms.app.ui.lists.MyListDetailsScreen
import com.bookworms.app.ui.lists.MyListsScreen
import com.bookworms.app.ui.lists.Shelf
import com.bookworms.app.ui.lists.ShelfScreen
import com.bookworms.app.ui.lists.sampleCustomLists
import com.bookworms.app.ui.lists.sampleListBooks
import com.bookworms.app.ui.loans.LoansScreen
import com.bookworms.app.ui.loans.QueueSpot
import com.bookworms.app.ui.loans.cancelRequest
import com.bookworms.app.ui.loans.hasSentRequest
import com.bookworms.app.ui.loans.queueSpots
import com.bookworms.app.ui.loans.sendRequest
import com.bookworms.app.ui.notifications.NotificationsScreen
import com.bookworms.app.ui.profile.ChangePasswordScreen
import com.bookworms.app.ui.profile.EditProfileScreen
import com.bookworms.app.ui.profile.ProfileScreen
import com.bookworms.app.ui.scan.ScanIsbnScreen
import com.bookworms.app.ui.scan.fetchBookDetailsByIsbn

private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    const val FORGOT_PASSWORD = "forgot_password"
    const val BOOK_DETAILS = "book_details/{isbn}?guest={guest}"
    const val ADD_COMMENT = "add_comment/{isbn}?group={group}&replyTo={replyTo}"
    const val SCAN_ISBN = "scan_isbn"
    const val INVITE_TO_GROUP = "invite_to_group"

    const val LIBRARY = "library"
    const val LOANS = "loans"
    const val LISTS = "lists"
    const val GROUPS = "groups"
    const val PROFILE = "profile"

    const val NOTIFICATIONS = "notifications"
    const val MY_BOOKS = "my_books"
    const val ADD_BOOK_MANUAL = "add_book_manual?isbn={isbn}"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val CHANGE_PASSWORD = "change_password"
    const val EDIT_PROFILE = "edit_profile"
    const val CREATE_LIST = "create_list"
    const val LIST_DETAILS = "list_details/{listName}"
    const val SHELF = "shelf/{shelf}"
    const val EDIT_LIST = "edit_list/{listName}"
    const val ADD_BOOKS_TO_LIST = "add_books_to_list/{listName}"
    const val GROUP_DETAILS = "group_details/{groupName}"
    const val EDIT_GROUP = "edit_group/{groupName}"
    const val MY_COPY = "my_copy/{title}"
    const val EDIT_OWN_BOOK = "edit_own_book/{title}"

    fun addBookManual(isbn: String? = null) = if (isbn == null) "add_book_manual" else "add_book_manual?isbn=$isbn"
    fun bookDetails(isbn: String, guest: Boolean = false) = if (guest) "book_details/$isbn?guest=true" else "book_details/$isbn"
    
    fun addComment(isbn: String, group: String? = null, replyTo: String? = null): String {
        val options = listOfNotNull(
            group?.let { "group=${Uri.encode(it)}" },
            replyTo?.let { "replyTo=${Uri.encode(it)}" },
        )
        return if (options.isEmpty()) "add_comment/$isbn" else "add_comment/$isbn?" + options.joinToString("&")
    }

    fun listDetails(listName: String) = "list_details/${Uri.encode(listName)}"
    fun shelf(shelf: Shelf) = "shelf/${shelf.name}"
    fun editList(listName: String) = "edit_list/${Uri.encode(listName)}"
    fun addBooksToList(listName: String) = "add_books_to_list/${Uri.encode(listName)}"
    fun groupDetails(groupName: String) = "group_details/${Uri.encode(groupName)}"
    fun editGroup(groupName: String) = "edit_group/${Uri.encode(groupName)}"
    fun myCopy(title: String) = "my_copy/${Uri.encode(title)}"
    fun editOwnBook(title: String) = "edit_own_book/${Uri.encode(title)}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val navigateBottomBar: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.LIBRARY) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openBookByTitle: (String) -> Unit = { title ->
        val book = sampleBooks.find { it.title == title } ?: sampleBooks.first()
        navController.navigate(Routes.bookDetails(book.isbn))
    }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            var email by rememberSaveable { mutableStateOf("") }
            var password by rememberSaveable { mutableStateOf("") }

            LoginScreen(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onLoginClick = {
                    navController.navigate(Routes.LIBRARY) {
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
            val results = remember(query) { filterBooks(sampleBooks, query) }

            BrowseScreen(
                query = query,
                results = results,
                onQueryChange = { query = it },
                onBookClick = { book -> navController.navigate(Routes.bookDetails(book.isbn, guest = true)) },
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
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            var email by rememberSaveable { mutableStateOf("") }
            var linkSent by rememberSaveable { mutableStateOf(false) }

            ForgotPasswordScreen(
                email = email,
                onEmailChange = { email = it },
                linkSent = linkSent,
                onSendLinkClick = { linkSent = true },
                onUseDifferentEmailClick = { linkSent = false },
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                onNavigate = navigateBottomBar,
                onBookClick = { book -> navController.navigate(Routes.bookDetails(book.isbn)) },
                onScanIsbnClick = { navController.navigate(Routes.SCAN_ISBN) },
            )
        }

        composable(Routes.LOANS) {
            LoansScreen(onNavigate = navigateBottomBar, onBookClick = openBookByTitle)
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
                onBookClick = openBookByTitle,
                onSeeAllClick = { shelf -> navController.navigate(Routes.shelf(shelf)) },
            )
        }

        composable(Routes.SHELF) { backStackEntry ->
            val shelf = Shelf.valueOf(backStackEntry.arguments?.getString("shelf") ?: Shelf.Favorites.name)
            ShelfScreen(
                shelf = shelf,
                onBookClick = openBookByTitle,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigate = navigateBottomBar,
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToMyBooks = { navController.navigate(Routes.MY_BOOKS) },
                photoUri = profilePhotoUri,
                onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                onChangePasswordClick = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onLogoutClick = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CREATE_LIST) {
            CreateListScreen(
                onCreateClick = { listName ->
                    navController.navigate(Routes.listDetails(listName)) {
                        popUpTo(Routes.CREATE_LIST) { inclusive = true }
                    }
                },
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.LIST_DETAILS) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString("listName").orEmpty()
            MyListDetailsScreen(
                listName = listName,
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onEditClick = { navController.navigate(Routes.editList(listName)) },
                onAddBooksClick = { navController.navigate(Routes.addBooksToList(listName)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_LIST) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString("listName").orEmpty()
            EditListScreen(
                listName = listName,
                onSaveClick = { newName ->
                    navController.navigate(Routes.listDetails(newName)) {
                        popUpTo(Routes.LIST_DETAILS) { inclusive = true }
                    }
                },
                onDeleteClick = { navController.popBackStack(Routes.LISTS, inclusive = false) },
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.ADD_BOOKS_TO_LIST) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString("listName").orEmpty()
            AddBooksToListScreen(
                listName = listName,
                isbnsOnList = sampleListBooks.map { it.book.isbn },
                onAddClick = { navController.popBackStack() },
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.GROUP_DETAILS) { backStackEntry ->
            val groupName = backStackEntry.arguments?.getString("groupName").orEmpty()
            GroupDetailsScreen(
                groupName = groupName,
                isAdmin = groupName == "Czytelnia 23",
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onInviteClick = { navController.navigate(Routes.INVITE_TO_GROUP) },
                onEditClick = { navController.navigate(Routes.editGroup(groupName)) },
                onLeaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_GROUP) { backStackEntry ->
            val groupKey = backStackEntry.arguments?.getString("groupName").orEmpty()
            val group = groupInfo(groupKey)
            var name by rememberSaveable { mutableStateOf(group.name) }
            var description by rememberSaveable { mutableStateOf(group.description) }
            var visibility by rememberSaveable { mutableStateOf(group.visibility) }

            CreateGroupScreen(
                name = name,
                description = description,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                visibility = visibility,
                onVisibilityChange = { visibility = it },
                invitedUsernames = emptyList(),
                onAddInvite = {},
                onRemoveInvite = {},
                onCreateGroupClick = {
                    sampleGroups[groupKey] = GroupInfo(name.trim(), description.trim(), visibility)
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
                onDeleteClick = { navController.popBackStack(Routes.GROUPS, inclusive = false) },
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onSaveClick = { photo ->
                    profilePhotoUri = photo
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
                photoUri = profilePhotoUri,
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(
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
                onAddBookClick = { navController.navigate(Routes.addBookManual()) },
                onBookClick = { title -> navController.navigate(Routes.myCopy(title)) },
            )
        }

        composable(Routes.MY_COPY) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            MyCopyScreen(
                title = title,
                onEditClick = { navController.navigate(Routes.editOwnBook(title)) },
                onOpenBookPageClick = { openBookByTitle(title) },
                onRemoveClick = {
                    sampleOwnBooks.removeAll { it.title == title }
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.EDIT_OWN_BOOK) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title").orEmpty()
            val context = LocalContext.current
            AddBookManualScreen(
                onSaveBook = { titleArg, authorArg, isbnArg, publisherArg, yearArg ->
                    val index = sampleOwnBooks.indexOfFirst { it.title == title }
                    if (index >= 0) {
                        sampleOwnBooks[index] = OwnBook(
                            title = titleArg,
                            author = authorArg,
                            year = yearArg,
                            isbn = isbnArg,
                            publisher = publisherArg,
                            groups = myGroups,
                        )
                    }
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            Routes.ADD_BOOK_MANUAL,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn")
            val context = LocalContext.current
            AddBookManualScreen(
                onScanIsbnClick = { navController.navigate(Routes.SCAN_ISBN) }, // <--- Tutaj uruchamiamy skaner z formularza
                onBackClick = { navController.popBackStack() },
                onSaveBook = { titleArg, authorArg, isbnArg, publisherArg, yearArg ->
                    if (sampleOwnBooks.any { it.title == titleArg }) {
                        Toast.makeText(context, R.string.already_in_my_books, Toast.LENGTH_SHORT).show()
                    } else {
                        val newBook = OwnBook(
                            title = titleArg,
                            author = authorArg,
                            year = yearArg,
                            isbn = isbnArg,
                            publisher = publisherArg,
                            groups = myGroups,
                        )
                        sampleOwnBooks.add(newBook)
                        Toast.makeText(context, R.string.book_added, Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(Routes.JOIN_GROUP) {
            JoinGroupScreen(
                onGroupJoined = { groupName ->
                    navController.navigate(Routes.groupDetails(groupName)) {
                        popUpTo(Routes.JOIN_GROUP) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATE_GROUP) {
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
                onCreateGroupClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            Routes.BOOK_DETAILS,
            arguments = listOf(navArgument("guest") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn")
            val isGuest = entry.arguments?.getBoolean("guest") ?: false
            val book = remember(isbn) { sampleBooks.first { it.isbn == isbn } }

            var isFavorite by rememberSaveable { mutableStateOf(false) }
            var readingStatus by rememberSaveable { mutableStateOf(ReadingStatus.TO_READ) }

            val details = sampleBookDetailsFor(book)
            val context = LocalContext.current

            BookDetailsScreen(
                book = details,
                isFavorite = isFavorite,
                readingStatus = readingStatus,
                onFavoriteClick = { isFavorite = !isFavorite },
                onReadingStatusChange = { readingStatus = it },
                requestedFrom = details.copies.map { it.ownerName }.filter { hasSentRequest(book.title, it) },
                queuedFor = queueSpots.filter { it.title == book.title }.map { it.owner },
                onSendRequest = { copy -> sendRequest(book.title, copy.ownerName) },
                onCancelRequest = { copy -> cancelRequest(book.title, copy.ownerName) },
                onJoinQueue = { copy -> queueSpots.add(QueueSpot(book.title, copy.ownerName)) },
                onLeaveQueue = { copy -> queueSpots.remove(QueueSpot(book.title, copy.ownerName)) },
                customLists = sampleCustomLists,
                onShareClick = { shareBook(context, details) },
                onOwnBookClick = { navController.navigate(Routes.addBookManual(book.isbn)) },
                onAddCommentClick = { groupName -> navController.navigate(Routes.addComment(book.isbn, group = groupName)) },
                onReplyClick = { groupName, comment ->
                    navController.navigate(Routes.addComment(book.isbn, group = groupName, replyTo = comment.id))
                },
                onBackClick = { navController.popBackStack() },
                isGuest = isGuest,
                onLoginClick = { navController.popBackStack(Routes.LOGIN, inclusive = false) },
            )
        }

        composable(
            Routes.ADD_COMMENT,
            arguments = listOf(
                navArgument("group") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("replyTo") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn")
            val group = entry.arguments?.getString("group")
            val replyTo = entry.arguments?.getString("replyTo")
            val book = remember(isbn) { sampleBookDetailsFor(sampleBooks.first { it.isbn == isbn }) }

            AddCommentScreen(
                bookTitle = book.title,
                bookAuthor = book.author,
                groups = book.groupDiscussions.map { it.groupName },
                initialGroup = group,
                onPostClick = { _, _, _, _ -> navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
                replyingTo = replyTo?.let { book.findGroupComment(it) },
            )
        }

        composable(Routes.SCAN_ISBN) {
            var scannedIsbn by rememberSaveable { mutableStateOf<String?>(null) }
            val context = LocalContext.current

            LaunchedEffect(scannedIsbn) {
                val code = scannedIsbn ?: return@LaunchedEffect
                val fetched = fetchBookDetailsByIsbn(
                    isbn = code,
                    ownerName = "Anna K.",
                    userGroups = myGroups,
                )

                if (sampleOwnBooks.none { it.isbn == fetched.isbn || it.title == fetched.title }) {
                    sampleOwnBooks.add(
                        0,
                        OwnBook(
                            title = fetched.title,
                            author = fetched.author,
                            year = fetched.year,
                            isbn = fetched.isbn,
                            publisher = "OpenLibrary",
                            groups = myGroups,
                        ),
                    )
                    Toast.makeText(context, R.string.book_added, Toast.LENGTH_SHORT).show()
                }

                navController.navigate(Routes.myCopy(fetched.title)) {
                    popUpTo(Routes.SCAN_ISBN) { inclusive = true }
                }
            }

            ScanIsbnScreen(
                recognizedIsbn = scannedIsbn,
                onIsbnRecognized = { code ->
                    if (scannedIsbn == null) {
                        scannedIsbn = code
                    }
                },
                onEnterIsbnManuallyClick = {
                    navController.navigate(Routes.addBookManual()) {
                        popUpTo(Routes.SCAN_ISBN) { inclusive = true }
                    }
                },
                onAddWithoutIsbnClick = {
                    navController.navigate(Routes.addBookManual()) {
                        popUpTo(Routes.SCAN_ISBN) { inclusive = true }
                    }
                },
                onCloseClick = { navController.popBackStack() },
            )
        }

        composable(Routes.INVITE_TO_GROUP) {
            val context = LocalContext.current
            var code by rememberSaveable { mutableStateOf(sampleInvite.code) }
            val invite = sampleInvite.withCode(code)

            InviteToGroupScreen(
                invite = invite,
                recentMembers = sampleRecentMembers,
                onCopyCodeClick = { copyInviteCode(context, invite.code) },
                onShareLinkClick = { shareInviteLink(context, invite) },
                onGenerateNewCodeClick = { code = newInviteCode() },
                onCloseClick = { navController.popBackStack() },
            )
        }
    }
}