@Composable
fun GroupsScreen(
    onNavigate: (String) -> Unit,
    onCreateGroupClick: () -> Unit // DODANO: Callback do tworzenia grupy
) {
    Scaffold(
        modifier = Modifier.mistedBackground(),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.groups_title), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        // DODANO: Ikonka "Plus" do tworzenia nowej grupy
                        IconButton(onClick = onCreateGroupClick) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.create_group_title) // Oczekuje na ten klucz w strings.xml
                            )
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* Otwórz ekran szukania grup */ },
                icon = { Icon(painterResource(R.drawable.ic_search), null) }, // Zmieniłem ikonę na lupę, by odróżnić od plusa
                text = { Text(stringResource(R.string.join_group)) }
            )
        },
        bottomBar = { AppBottomBar("groups", onNavigate) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { GroupCard(name = "Czytelnia 23", members = "14 members", role = "admin") }
            item { GroupCard(name = "Rodzina", members = "5 members", role = "member") }
        }
    }
}