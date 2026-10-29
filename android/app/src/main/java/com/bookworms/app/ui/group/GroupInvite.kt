package com.bookworms.app.ui.group

// An invite code that lets people join a group.
data class GroupInvite(
    val groupName: String,
    val code: String,
    // The same invite as a link, for sending in a chat.
    val link: String,
    val daysValid: Int,
    val usesLeft: Int,
)

data class RecentMember(
    val name: String,
    // When they joined. Plain text for now; it'll be a real date once it comes from the server.
    val joined: String,
)

// Temporary data until invites come from the server.
val sampleInvite = GroupInvite(
    groupName = "Czytelnia 23",
    code = "BKX-7F2Q",
    // TODO: the real link once the app has a domain.
    link = "https://bookworms.example.com/join/BKX-7F2Q",
    daysValid = 7,
    usesLeft = 14,
)

val sampleRecentMembers = listOf(
    RecentMember("Marek Wrona", joined = "today, 9:12"),
    RecentMember("Ola Nowak", joined = "yesterday"),
)
