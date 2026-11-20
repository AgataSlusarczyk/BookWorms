package com.bookworms.server.groups

import java.util.UUID

// Told to the rest of the server after the change is saved, e.g. so the chat stops sending this person
// the group's messages (see ChatHub).

// Left on their own, or removed by the admin.
data class MemberLeftGroup(val userId: UUID, val groupId: UUID)

data class GroupDeleted(val groupId: UUID)
