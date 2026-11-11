package com.bookworms.app.ui.group

// An invite code that lets people join a group.
data class GroupInvite(
    val groupName: String,
    val code: String,
    // The same invite as a link, for sending in a chat.
    val link: String,
)

data class RecentMember(
    val name: String,
    // When they joined, e.g. "Joined 11.11.2026".
    val joined: String,
    val isAdmin: Boolean = false,
)

// TODO: a real link that opens the app, once the app has a domain. For now the code is what counts.
fun inviteLink(code: String) = "https://bookworms.example.com/join/$code"
