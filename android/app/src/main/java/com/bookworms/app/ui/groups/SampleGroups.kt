package com.bookworms.app.ui.groups

import androidx.compose.runtime.mutableStateMapOf

// What the admin can change on Edit group.
internal data class GroupInfo(val name: String, val description: String, val visibility: GroupVisibility)

// TODO: groups from the server.
// Kept outside the screens, so a change saved on Edit group shows up in Group details too.
// The key is the group's name from the Groups list – it stays the same after a rename.
internal val sampleGroups = mutableStateMapOf(
    "Czytelnia 23" to GroupInfo("Czytelnia 23", "Sci-fi fans from work. We meet every first Thursday.", GroupVisibility.Private),
    "Rodzina" to GroupInfo("Rodzina", "Books we pass around the family.", GroupVisibility.Private),
)

internal fun groupInfo(key: String): GroupInfo = sampleGroups[key] ?: GroupInfo(key, "", GroupVisibility.Private)
