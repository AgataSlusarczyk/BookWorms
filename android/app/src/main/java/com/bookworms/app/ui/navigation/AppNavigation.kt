package com.bookworms.app.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookworms.app.R
import com.bookworms.app.data.AuthRepository
import com.bookworms.app.data.auth.Session
import com.bookworms.app.ui.auth.ForgotPasswordScreen
import com.bookworms.app.ui.auth.LoginError
import com.bookworms.app.ui.auth.LoginScreen
import com.bookworms.app.ui.auth.LoginViewModel
import com.bookworms.app.ui.auth.RegisterError
import com.bookworms.app.ui.auth.RegisterScreen
import com.bookworms.app.ui.auth.RegisterViewModel
import com.bookworms.app.ui.book.AddCommentScreen
import com.bookworms.app.ui.book.BookDetailsScreen
import com.bookworms.app.ui.book.BookDetailsViewModel
import com.bookworms.app.ui.book.ReadingStatus
import com.bookworms.app.ui.book.findGroupComment
import com.bookworms.app.ui.book.shareBook
import com.bookworms.app.ui.browse.BrowseScreen
import com.bookworms.app.ui.browse.BrowseViewModel
import com.bookworms.app.ui.browse.sampleBooks
import com.bookworms.app.ui.components.LoadStateScreen
import com.bookworms.app.ui.components.profilePhotoUri
import com.bookworms.app.ui.groups.CreateGroupScreen
import com.bookworms.app.ui.groups.CreateGroupViewModel
import com.bookworms.app.ui.groups.GroupDetailsViewModel
import com.bookworms.app.ui.groups.GroupEvent
import com.bookworms.app.ui.groups.GroupsViewModel
import com.bookworms.app.ui.groups.JoinGroupViewModel
import com.bookworms.app.ui.groups.GroupDetailsScreen
import com.bookworms.app.ui.groups.GroupsScreen
import com.bookworms.app.ui.groups.JoinGroupScreen
import com.bookworms.app.ui.group.GroupInvite
import com.bookworms.app.ui.group.InviteToGroupScreen
import com.bookworms.app.ui.group.RecentMember
import com.bookworms.app.ui.group.inviteLink
import com.bookworms.app.ui.group.copyInviteCode
import com.bookworms.app.ui.group.shareInviteLink
import com.bookworms.app.ui.library.AddBookManualScreen
import com.bookworms.app.ui.library.LibraryScreen
import com.bookworms.app.ui.library.LibraryViewModel
import com.bookworms.app.ui.library.MyBooksScreen
import com.bookworms.app.ui.library.MyCopyScreen
import com.bookworms.app.ui.library.AddBookEvent
import com.bookworms.app.ui.library.AddBookViewModel
import com.bookworms.app.ui.library.MyBooksEvent
import com.bookworms.app.ui.library.MyBooksViewModel
import com.bookworms.app.ui.library.toForm
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
import com.bookworms.app.ui.loans.LoansViewModel
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
import kotlinx.coroutines.flow.Flow
import java.time.format.DateTimeFormatter

// The names of the screens the app can go to.
private object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val BROWSE = "browse"
    const val FORGOT_PASSWORD = "forgot_password"
    // {isbn} is filled in with the book to show, e.g. "book_details/9788308075197".
    // The part after "?" is optional: guest=true when someone browses without an account.
    const val BOOK_DETAILS = "book_details/{isbn}?guest={guest}"
    // group = where the comment goes (none = Public), replyTo = the id of the comment being answered.
    const val ADD_COMMENT = "add_comment/{isbn}?group={group}&replyTo={replyTo}"
    const val SCAN_ISBN = "scan_isbn"
    const val INVITE_TO_GROUP = "invite_to_group/{groupId}"

    // Tabs of the bottom bar – the same names AppBottomBar sends in onNavigate.
    const val LIBRARY = "library"
    const val LOANS = "loans"
    const val LISTS = "lists"
    const val GROUPS = "groups"
    const val PROFILE = "profile"

    // Screens opened from a tab, without the bottom bar.
    const val NOTIFICATIONS = "notifications"
    const val MY_BOOKS = "my_books"
    // isbn = a book from the app to fill the form in with (none = an empty form).
    const val ADD_BOOK_MANUAL = "add_book_manual?isbn={isbn}"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val CHANGE_PASSWORD = "change_password"
    const val EDIT_PROFILE = "edit_profile"
    const val CREATE_LIST = "create_list"
    const val LIST_DETAILS = "list_details/{listName}"
    // {shelf} = Favorites, WantToRead or Read.
    const val SHELF = "shelf/{shelf}"
    const val EDIT_LIST = "edit_list/{listName}"
    const val ADD_BOOKS_TO_LIST = "add_books_to_list/{listName}"
    // {groupId} = the group's id on the server.
    const val GROUP_DETAILS = "group_details/{groupId}"
    const val EDIT_GROUP = "edit_group/{groupId}"
    // {copyId} = the copy's id on the server.
    const val MY_COPY = "my_copy/{copyId}"
    const val EDIT_OWN_BOOK = "edit_own_book/{copyId}"

    fun addBookManual(isbn: String? = null) = if (isbn == null) "add_book_manual" else "add_book_manual?isbn=$isbn"

    fun bookDetails(isbn: String, guest: Boolean = false) = if (guest) "book_details/$isbn?guest=true" else "book_details/$isbn"

    fun addComment(isbn: String, group: String? = null, replyTo: String? = null): String {
        // Leaving a part out means "none" – the screen then gets null.
        val options = listOfNotNull(
            group?.let { "group=${Uri.encode(it)}" },
            replyTo?.let { "replyTo=${Uri.encode(it)}" },
        )
        return if (options.isEmpty()) "add_comment/$isbn" else "add_comment/$isbn?" + options.joinToString("&")
    }

    // Uri.encode turns spaces and other special characters into a form that fits in a route.
    fun listDetails(listName: String) = "list_details/${Uri.encode(listName)}"
    fun shelf(shelf: Shelf) = "shelf/${shelf.name}"
    fun editList(listName: String) = "edit_list/${Uri.encode(listName)}"
    fun addBooksToList(listName: String) = "add_books_to_list/${Uri.encode(listName)}"
    fun groupDetails(groupId: String) = "group_details/$groupId"
    fun editGroup(groupId: String) = "edit_group/$groupId"
    fun inviteToGroup(groupId: String) = "invite_to_group/$groupId"
    fun myCopy(copyId: String) = "my_copy/$copyId"
    fun editOwnBook(copyId: String) = "edit_own_book/$copyId"
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

    // Temporary – most screens only know a book's title, so we look it up in the sample books.
    // A title that isn't there (e.g. "Pachnidło") opens the first sample book.
    val openBookByTitle: (String) -> Unit = { title ->
        val book = sampleBooks.find { it.title == title } ?: sampleBooks.first()
        navController.navigate(Routes.bookDetails(book.isbn))
    }

    val user by Session.user.collectAsState()
    // Remembered together with the first screen, so a logout that happens before the check below runs
    // (e.g. the first request of the Library already got 401) is still noticed.
    var wasLoggedIn by remember { mutableStateOf(user != null) }

    // Someone who logged in before goes straight to the Library. Checked once, when the app opens.
    val startDestination = remember { if (wasLoggedIn) Routes.LIBRARY else Routes.LOGIN }

    // The server stopped accepting the token (see ApiClient) – back to login, with no way "back" into the app.
    LaunchedEffect(user) {
        if (wasLoggedIn && user == null && navController.currentDestination?.route != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
        wasLoggedIn = user != null
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            val viewModel: LoginViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            // The server accepted the email and password.
            LaunchedEffect(state.loggedIn) {
                if (state.loggedIn) {
                    navController.navigate(Routes.LIBRARY) {
                        // Removes login from the pile, so "back" doesn't return to it.
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                email = state.email,
                password = state.password,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                errorMessage = when (state.error) {
                    LoginError.InvalidCredentials -> stringResource(R.string.error_login_invalid)
                    LoginError.ServerUnreachable -> stringResource(R.string.error_server_unreachable)
                    null -> null
                },
                loggingIn = state.loggingIn,
                loginEnabled = state.canLogIn,
                onLoginClick = viewModel::login,
                onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
                onBrowseWithoutAccountClick = { navController.navigate(Routes.BROWSE) },
            )
        }

        composable(Routes.BROWSE) {
            // Searches the catalog on the server.
            val viewModel: BrowseViewModel = viewModel()
            val query by viewModel.query.collectAsState()
            val results by viewModel.results.collectAsState()

            BrowseScreen(
                query = query,
                results = results,
                onQueryChange = viewModel::onQueryChange,
                onRetryClick = viewModel::retry,
                onBookClick = { book -> navController.navigate(Routes.bookDetails(book.isbn, guest = true)) },
                // Login is already under this screen, so we just go back to it.
                onLoginClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.REGISTER) {
            val viewModel: RegisterViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            // The account is created and the user is logged in.
            LaunchedEffect(state.registered) {
                if (state.registered) {
                    navController.navigate(Routes.LIBRARY) {
                        // Removes sign-up and login from the pile, so "back" doesn't return to them.
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                name = state.name,
                email = state.email,
                password = state.password,
                repeatPassword = state.repeatPassword,
                onNameChange = viewModel::onNameChange,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onRepeatPasswordChange = viewModel::onRepeatPasswordChange,
                termsAccepted = state.termsAccepted,
                onTermsAcceptedChange = viewModel::onTermsAcceptedChange,
                emailError = if (state.emailInvalid) stringResource(R.string.error_email_invalid) else null,
                passwordError = if (state.passwordTooWeak) stringResource(R.string.error_password_too_weak) else null,
                repeatPasswordError = if (state.passwordsDontMatch) stringResource(R.string.error_passwords_dont_match) else null,
                errorMessage = when (state.error) {
                    RegisterError.EmailTaken -> stringResource(R.string.error_email_taken)
                    RegisterError.InvalidData -> stringResource(R.string.error_sign_up_invalid)
                    RegisterError.ServerUnreachable -> stringResource(R.string.error_server_unreachable)
                    null -> null
                },
                creatingAccount = state.creatingAccount,
                createAccountEnabled = state.canCreateAccount,
                onCreateAccountClick = viewModel::createAccount,
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
            val viewModel: LibraryViewModel = viewModel()
            val books by viewModel.books.collectAsState()

            LibraryScreen(
                books = books,
                onRetryClick = viewModel::refresh,
                onNavigate = navigateBottomBar,
                onBookClick = { book -> navController.navigate(Routes.bookDetails(book.isbn)) },
            )
        }

        composable(Routes.LOANS) {
            val viewModel: LoansViewModel = viewModel()
            val loans by viewModel.loans.collectAsState()
            val saving by viewModel.saving.collectAsState()
            val context = LocalContext.current

            // Each time the tab opens – a request may have come in since it was last seen.
            LaunchedEffect(Unit) { viewModel.refresh() }
            LaunchedEffect(viewModel) {
                viewModel.errors.collect { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            }

            LoansScreen(
                loans = loans,
                saving = saving,
                onRetryClick = viewModel::refresh,
                onMarkReturned = viewModel::markReturned,
                onAccept = viewModel::accept,
                onReject = viewModel::reject,
                onNavigate = navigateBottomBar,
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
            )
        }

        composable(Routes.GROUPS) {
            val viewModel: GroupsViewModel = viewModel()
            val groups by viewModel.groups.collectAsState()
            // Each time the tab opens – a group may have been made, joined or left since.
            LaunchedEffect(Unit) { viewModel.refresh() }
            GroupsScreen(
                groups = groups,
                onRetryClick = viewModel::refresh,
                onNavigate = navigateBottomBar,
                onCreateGroupClick = { navController.navigate(Routes.CREATE_GROUP) },
                onJoinGroupClick = { navController.navigate(Routes.JOIN_GROUP) },
                onGroupClick = { group -> navController.navigate(Routes.groupDetails(group.id)) },
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
            // The route has the shelf's name, e.g. "WantToRead"; valueOf turns it back into Shelf.WantToRead.
            val shelf = Shelf.valueOf(backStackEntry.arguments?.getString("shelf") ?: Shelf.Favorites.name)
            ShelfScreen(
                shelf = shelf,
                onBookClick = openBookByTitle,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                userName = user?.name.orEmpty(),
                userEmail = user?.email.orEmpty(),
                onNavigate = navigateBottomBar,
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToMyBooks = { navController.navigate(Routes.MY_BOOKS) },
                photoUri = profilePhotoUri,
                onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                onChangePasswordClick = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onLogoutClick = {
                    AuthRepository().logout()
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
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.LIST_DETAILS) { backStackEntry ->
            // The route gives the name back already decoded, e.g. "Holiday reads".
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
                // Opens the list again under its (maybe new) name. popUpTo removes the old list
                // and this form from the pile, so Back goes to My lists.
                onSaveClick = { newName ->
                    navController.navigate(Routes.listDetails(newName)) {
                        popUpTo(Routes.LIST_DETAILS) { inclusive = true }
                    }
                },
                // The list is gone, so we go all the way back to My lists.
                onDeleteClick = { navController.popBackStack(Routes.LISTS, inclusive = false) },
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.ADD_BOOKS_TO_LIST) { backStackEntry ->
            val listName = backStackEntry.arguments?.getString("listName").orEmpty()
            AddBooksToListScreen(
                listName = listName,
                // TODO: the books on this list from the server.
                isbnsOnList = sampleListBooks.map { it.book.isbn },
                // TODO: add the books on the server first. For now it just goes back to the list.
                onAddClick = { navController.popBackStack() },
                onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.GROUP_DETAILS) { entry ->
            val viewModel = groupDetailsViewModel(navController, entry)
            val state by viewModel.group.collectAsState()
            val books by viewModel.books.collectAsState()
            val saving by viewModel.saving.collectAsState()
            HandleGroupEvents(viewModel.events, navController)

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_group_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { group ->
                GroupDetailsScreen(
                    group = group,
                    books = books,
                    saving = saving,
                    onRetryBooksClick = viewModel::refresh,
                    onBookClick = { isbn -> navController.navigate(Routes.bookDetails(isbn)) },
                    onInviteClick = group.inviteCode?.let { { navController.navigate(Routes.inviteToGroup(group.id)) } },
                    onEditClick = { navController.navigate(Routes.editGroup(group.id)) },
                    onMakeAdmin = viewModel::makeAdmin,
                    onRemoveMember = viewModel::removeMember,
                    onLeaveClick = viewModel::leave,
                    onBackClick = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.EDIT_GROUP) { entry ->
            val viewModel = groupDetailsViewModel(navController, entry)
            val state by viewModel.group.collectAsState()
            val saving by viewModel.saving.collectAsState()
            HandleGroupEvents(viewModel.events, navController)

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_group_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { group ->
                // Starts with the group's current details.
                var name by rememberSaveable { mutableStateOf(group.name) }
                var description by rememberSaveable { mutableStateOf(group.description) }
                var membersCanInvite by rememberSaveable { mutableStateOf(group.membersCanInvite) }

                CreateGroupScreen(
                    name = name,
                    description = description,
                    onNameChange = { name = it },
                    onDescriptionChange = { description = it },
                    // Back on Group details once the server saved it (GroupEvent.Saved).
                    onCreateGroupClick = { viewModel.update(name, description, membersCanInvite) },
                    onBackClick = { navController.popBackStack() },
                    saving = saving,
                    membersCanInvite = membersCanInvite,
                    onMembersCanInviteChange = { membersCanInvite = it },
                    onDeleteClick = viewModel::delete,
                )
            }
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                // TODO: save the name and photo on the server first. For now the photo is kept until the app closes.
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
                // TODO: send the passwords to the server first. For now it just goes back to Profile.
                onSaveClick = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.MY_BOOKS) { entry ->
            val viewModel = myBooksViewModel(navController, entry)
            val books by viewModel.books.collectAsState()
            MyBooksScreen(
                books = books,
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
                onAddBookClick = { navController.navigate(Routes.addBookManual()) },
                onBookClick = { book -> navController.navigate(Routes.myCopy(book.id)) },
            )
        }

        composable(Routes.MY_COPY) { entry ->
            val copyId = entry.arguments?.getString("copyId").orEmpty()
            val viewModel = myBooksViewModel(navController, entry)
            val state by viewModel.books.collectAsState()
            val saving by viewModel.saving.collectAsState()
            val groups by viewModel.groups.collectAsState()
            ShowMyBooksErrors(viewModel)

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_books_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { books ->
                val book = books.find { it.id == copyId }
                if (book == null) {
                    // Removed (here, or on another phone) – back to My books.
                    CloseWhenGone(navController, entry)
                    return@LoadStateScreen
                }
                MyCopyScreen(
                    book = book,
                    groups = groups,
                    saving = saving,
                    onAvailableChange = { viewModel.setAvailable(book, it) },
                    onVisibleInChange = { groupId, visible -> viewModel.setVisibleIn(book, groupId, visible) },
                    onRetryGroupsClick = viewModel::refreshGroups,
                    onEditClick = if (book.canEditBook) {
                        { navController.navigate(Routes.editOwnBook(book.id)) }
                    } else {
                        null
                    },
                    onOpenBookPageClick = book.isbn?.let { isbn -> { navController.navigate(Routes.bookDetails(isbn)) } },
                    onRemoveClick = { viewModel.remove(book) },
                    onBackClick = { navController.popBackStack() },
                )
            }
        }

        composable(Routes.EDIT_OWN_BOOK) { entry ->
            val copyId = entry.arguments?.getString("copyId").orEmpty()
            val viewModel = myBooksViewModel(navController, entry)
            val state by viewModel.books.collectAsState()
            val saving by viewModel.saving.collectAsState()
            ShowMyBooksErrors(viewModel, onEdited = { navController.popBackStack() })

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_books_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { books ->
                val book = books.find { it.id == copyId }
                if (book == null) {
                    CloseWhenGone(navController, entry)
                    return@LoadStateScreen
                }
                AddBookManualScreen(
                    onScanIsbnClick = { navController.navigate(Routes.SCAN_ISBN) },
                    onBackClick = { navController.popBackStack() },
                    // Back on My copy once the server saved it (onEdited above).
                    onSaveClick = { form -> viewModel.editBook(book, form) },
                    editing = book.toForm(),
                    saving = saving,
                )
            }
        }

        composable(
            Routes.ADD_BOOK_MANUAL,
            arguments = listOf(navArgument("isbn") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn")
            val viewModel = viewModel { AddBookViewModel(isbn) }
            val prefill by viewModel.prefill.collectAsState()
            val saving by viewModel.saving.collectAsState()
            // My books may be open underneath (also when Add book was opened from a book's page on the way).
            // Then the new book goes straight onto its list.
            val myBooksEntry = remember(entry) { runCatching { navController.getBackStackEntry(Routes.MY_BOOKS) }.getOrNull() }
            val myBooks = myBooksEntry?.let { viewModel(it) { MyBooksViewModel() } }
            val context = LocalContext.current

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AddBookEvent.Added -> {
                            myBooks?.added(event.book)
                            Toast.makeText(context, R.string.book_added, Toast.LENGTH_SHORT).show()
                            // Back to where Add book was opened from: My books or the book's page.
                            navController.popBackStack()
                        }
                        is AddBookEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            LoadStateScreen(
                state = prefill,
                failedMessage = stringResource(R.string.load_book_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { form ->
                AddBookManualScreen(
                    onScanIsbnClick = { navController.navigate(Routes.SCAN_ISBN) },
                    onBackClick = { navController.popBackStack() },
                    onSaveClick = viewModel::save,
                    prefill = form,
                    saving = saving,
                )
            }
        }

        composable(Routes.JOIN_GROUP) {
            val viewModel: JoinGroupViewModel = viewModel()
            val found by viewModel.found.collectAsState()
            val notFound by viewModel.notFound.collectAsState()
            val busy by viewModel.saving.collectAsState()
            // Opens the new group. Join group goes off the stack, so Back goes to Groups, not here.
            HandleGroupEvents(viewModel.events, navController, closeOnOpen = Routes.JOIN_GROUP)

            JoinGroupScreen(
                found = found,
                notFound = notFound,
                busy = busy,
                onCodeChange = viewModel::onCodeChange,
                onFindClick = viewModel::find,
                onJoinClick = viewModel::join,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(Routes.CREATE_GROUP) {
            val viewModel: CreateGroupViewModel = viewModel()
            val saving by viewModel.saving.collectAsState()
            var name by rememberSaveable { mutableStateOf("") }
            var description by rememberSaveable { mutableStateOf("") }
            // Opens the new group. Create group goes off the stack, so Back goes to Groups, not here.
            HandleGroupEvents(viewModel.events, navController, closeOnOpen = Routes.CREATE_GROUP)

            CreateGroupScreen(
                name = name,
                description = description,
                onNameChange = { name = it },
                onDescriptionChange = { description = it },
                onCreateGroupClick = { viewModel.create(name, description) },
                onBackClick = { navController.popBackStack() },
                saving = saving,
            )
        }

        composable(
            Routes.BOOK_DETAILS,
            arguments = listOf(navArgument("guest") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn").orEmpty()
            val isGuest = entry.arguments?.getBoolean("guest") ?: false
            // Loads the book from the server. Copies and comments are still sample data.
            val viewModel = viewModel { BookDetailsViewModel(isbn) }
            val state by viewModel.book.collectAsState()

            // Temporary state – moves to a ViewModel when favorites and reading status are on the server.
            var isFavorite by rememberSaveable { mutableStateOf(false) }
            var readingStatus by rememberSaveable { mutableStateOf(ReadingStatus.TO_READ) }
            val context = LocalContext.current

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_book_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { book ->
                BookDetailsScreen(
                    book = book,
                    isFavorite = isFavorite,
                    readingStatus = readingStatus,
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onReadingStatusChange = { readingStatus = it },
                    // TODO: send requests and queue changes to the server.
                    // For now they go to the shared sample lists, so a request shows up in Loans too.
                    requestedFrom = book.copies.map { it.ownerName }.filter { hasSentRequest(book.title, it) },
                    queuedFor = queueSpots.filter { it.title == book.title }.map { it.owner },
                    onSendRequest = { copy -> sendRequest(book.title, copy.ownerName) },
                    onCancelRequest = { copy -> cancelRequest(book.title, copy.ownerName) },
                    onJoinQueue = { copy -> queueSpots.add(QueueSpot(book.title, copy.ownerName)) },
                    onLeaveQueue = { copy -> queueSpots.remove(QueueSpot(book.title, copy.ownerName)) },
                    customLists = sampleCustomLists,
                    onShareClick = { shareBook(context, book) },
                    // Opens Add book with this book's details already filled in.
                    onOwnBookClick = { navController.navigate(Routes.addBookManual(book.isbn)) },
                    onAddCommentClick = { groupName -> navController.navigate(Routes.addComment(book.isbn, group = groupName)) },
                    onReplyClick = { groupName, comment ->
                        navController.navigate(Routes.addComment(book.isbn, group = groupName, replyTo = comment.id))
                    },
                    onBackClick = { navController.popBackStack() },
                    isGuest = isGuest,
                    // Login is at the bottom of the pile (under Browse), so we go back down to it.
                    onLoginClick = { navController.popBackStack(Routes.LOGIN, inclusive = false) },
                )
            }
        }

        composable(
            Routes.ADD_COMMENT,
            arguments = listOf(
                navArgument("group") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("replyTo") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val isbn = entry.arguments?.getString("isbn").orEmpty()
            val group = entry.arguments?.getString("group")
            val replyTo = entry.arguments?.getString("replyTo")
            // The same book as on Book details, for its title and author.
            val viewModel = viewModel { BookDetailsViewModel(isbn) }
            val state by viewModel.book.collectAsState()

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_book_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { book ->
                AddCommentScreen(
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    groups = book.groupDiscussions.map { it.groupName },
                    initialGroup = group,
                    // TODO: show the new comment in Book details once comments come from the server.
                    onPostClick = { _, _, _, _ -> navController.popBackStack() },
                    onBackClick = { navController.popBackStack() },
                    replyingTo = replyTo?.let { book.findGroupComment(it) },
                )
            }
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

        composable(Routes.INVITE_TO_GROUP) { entry ->
            val viewModel = groupDetailsViewModel(navController, entry)
            val state by viewModel.group.collectAsState()
            val saving by viewModel.saving.collectAsState()
            // Needed for the clipboard and the share menu.
            val context = LocalContext.current
            HandleGroupEvents(viewModel.events, navController)

            LoadStateScreen(
                state = state,
                failedMessage = stringResource(R.string.load_group_failed),
                onRetryClick = viewModel::refresh,
                onBackClick = { navController.popBackStack() },
            ) { group ->
                val code = group.inviteCode
                if (code == null) {
                    // The admin stopped letting members invite in the meantime.
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@LoadStateScreen
                }
                val invite = GroupInvite(groupName = group.name, code = code, link = inviteLink(code))
                InviteToGroupScreen(
                    invite = invite,
                    // The newest members first – the server sends the earliest first.
                    recentMembers = group.members.asReversed().take(RecentMembersCount).map {
                        RecentMember(it.name, stringResource(R.string.member_joined, it.joined.format(JoinedDate)), it.isAdmin)
                    },
                    onCopyCodeClick = { copyInviteCode(context, invite.code) },
                    onShareLinkClick = { shareInviteLink(context, invite) },
                    onGenerateNewCodeClick = viewModel::newInviteCode,
                    onCloseClick = { navController.popBackStack() },
                    saving = saving,
                )
            }
        }
    }
}

// My books, My copy and Edit book share one ViewModel – the one of My books, which is always under the other two.
// So a change saved on one of them shows on the others right away.
@Composable
private fun myBooksViewModel(navController: NavController, entry: NavBackStackEntry): MyBooksViewModel {
    val owner = remember(entry) {
        if (entry.destination.route == Routes.MY_BOOKS) entry else navController.getBackStackEntry(Routes.MY_BOOKS)
    }
    return viewModel(owner) { MyBooksViewModel() }
}

// Shows what went wrong with a change (a short Toast). onEdited = what to do when Edit book was saved.
@Composable
private fun ShowMyBooksErrors(viewModel: MyBooksViewModel, onEdited: () -> Unit = {}) {
    val context = LocalContext.current
    val currentOnEdited by rememberUpdatedState(onEdited)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                MyBooksEvent.Edited -> currentOnEdited()
                is MyBooksEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// The book this screen shows is no longer in My books – close the screen (once, even while it slides away).
@Composable
private fun CloseWhenGone(navController: NavController, entry: NavBackStackEntry) {
    LaunchedEffect(entry) {
        if (navController.currentBackStackEntry == entry) navController.popBackStack()
    }
}

// Group details, Edit group and Invite share one ViewModel – the one of Group details, which is always under the
// other two. So a change saved on one of them shows on the others right away.
@Composable
private fun groupDetailsViewModel(navController: NavController, entry: NavBackStackEntry): GroupDetailsViewModel {
    val owner = remember(entry) {
        if (entry.destination.route == Routes.GROUP_DETAILS) entry else navController.getBackStackEntry(Routes.GROUP_DETAILS)
    }
    val groupId = owner.arguments?.getString("groupId").orEmpty()
    return viewModel(owner) { GroupDetailsViewModel(groupId) }
}

// What to do after a group change: open a group, go back, or show what went wrong.
// closeOnOpen = the screen that goes off the stack when a group opens (Create group, Join group).
@Composable
private fun HandleGroupEvents(events: Flow<GroupEvent>, navController: NavController, closeOnOpen: String? = null) {
    val context = LocalContext.current
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is GroupEvent.Opened -> navController.navigate(Routes.groupDetails(event.groupId)) {
                    if (closeOnOpen != null) popUpTo(closeOnOpen) { inclusive = true }
                }
                GroupEvent.Saved -> navController.popBackStack()
                // Left or deleted: back to the Groups tab, which loads the list again.
                GroupEvent.Gone -> if (!navController.popBackStack(Routes.GROUPS, inclusive = false)) {
                    navController.navigate(Routes.GROUPS) { popUpTo(Routes.LIBRARY) }
                }
                is GroupEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// How many of the newest members Invite shows.
private const val RecentMembersCount = 5

private val JoinedDate = DateTimeFormatter.ofPattern("dd.MM.yyyy")
