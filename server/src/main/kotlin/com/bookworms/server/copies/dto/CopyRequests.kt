package com.bookworms.server.copies.dto

import com.bookworms.server.copies.Availability
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

// "I have this book" – what the Add book form sends.
data class AddCopyRequest(
	// Optional. Dashes and spaces are fine, e.g. "978-83-08-04946-1". An old 10-digit ISBN works too.
	val isbn: String? = null,

	@field:NotBlank
	@field:Size(max = 500)
	val title: String,

	@field:NotBlank
	@field:Size(max = 1000)
	val authors: String,

	@field:Size(max = 255)
	val publisher: String? = null,

	val publishedYear: Int? = null,
)

// Edit book – only for a book the user typed in by hand. The ISBN can't be changed.
data class EditBookRequest(
	@field:NotBlank
	@field:Size(max = 500)
	val title: String,

	@field:NotBlank
	@field:Size(max = 1000)
	val authors: String,

	@field:Size(max = 255)
	val publisher: String? = null,

	val publishedYear: Int? = null,
)

data class ChangeAvailabilityRequest(
	val availability: Availability,
)

// "Visible in" on My copy: the groups that can see the copy. Empty = only the owner sees it.
data class ChangeGroupsRequest(
	val groupIds: Set<UUID>,
)
