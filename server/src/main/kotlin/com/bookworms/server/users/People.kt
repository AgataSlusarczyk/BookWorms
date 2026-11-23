package com.bookworms.server.users

import com.bookworms.server.loans.dto.PersonResponse
import java.util.UUID

// The people behind these ids, with their names and photos, for answers that show who's who. One query for all.
fun UserRepository.people(ids: Collection<UUID>): Map<UUID, PersonResponse> =
	findAllById(ids.toSet()).associate { it.id!! to PersonResponse(it.id!!, it.name, it.photoUrl) }

// Someone who isn't there (shouldn't happen) still comes with their id, just without a name.
fun Map<UUID, PersonResponse>.person(id: UUID): PersonResponse = this[id] ?: PersonResponse(id, "")
