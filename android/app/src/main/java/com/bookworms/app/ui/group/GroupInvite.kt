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

// A fresh code like "BKX-3M9T".
// TODO: the server makes the new code and turns the old one off.
fun newInviteCode(): String {
    // No 0/O and 1/I, so the code is easy to read out loud.
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return "BKX-" + (1..4).map { chars.random() }.joinToString("")
}

// The same invite with another code, and a link that ends with it.
fun GroupInvite.withCode(code: String): GroupInvite =
    copy(code = code, link = link.substringBeforeLast('/') + "/" + code)

val sampleRecentMembers = listOf(
    RecentMember("Marek Wrona", joined = "today, 9:12"),
    RecentMember("Ola Nowak", joined = "yesterday"),
)
