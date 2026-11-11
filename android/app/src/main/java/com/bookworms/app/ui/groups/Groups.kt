package com.bookworms.app.ui.groups

import java.time.LocalDate

// One group on the Groups list.
internal data class GroupSummary(
    val id: String,
    val name: String,
    val memberCount: Int,
    val isAdmin: Boolean,
)

// Everything Group details, Edit group and Invite show.
internal data class GroupDetails(
    val id: String,
    val name: String,
    val description: String,
    val isAdmin: Boolean,
    // Set by the admin: can ordinary members invite too?
    val membersCanInvite: Boolean,
    // null = the user can't invite, so there's no Invite button.
    val inviteCode: String?,
    // The earliest first.
    val members: List<GroupMember>,
)

internal data class GroupMember(
    val id: String,
    val name: String,
    val isAdmin: Boolean,
    val joined: LocalDate,
)

// One copy in the group's library.
internal data class GroupBook(
    val copyId: String,
    // null = typed in by hand, so it has no page to open.
    val isbn: String?,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val owner: String,
    // Who has it now. null = on the owner's shelf.
    val borrowedBy: String?,
    // false = the owner doesn't lend it now.
    val available: Boolean,
)

// The group behind a code, shown on Join group before joining.
internal data class InvitePreview(
    val name: String,
    val description: String,
    val memberCount: Int,
    val alreadyMember: Boolean,
)
