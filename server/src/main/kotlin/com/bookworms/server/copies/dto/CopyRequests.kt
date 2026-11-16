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

	@field:Size(max = MAX_DESCRIPTION)
	val description: String? = null,

	// true = the user already has this book and said "add another copy anyway".
	val addAnother: Boolean = false,
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

	@field:Size(max = MAX_DESCRIPTION)
	val description: String? = null,
)

// A few pages of text – more than any book's blurb.
const val MAX_DESCRIPTION = 5000

// "My description" on a book: the owner's own version of the description. Empty or null = back to the catalog's.
data class ChangeDescriptionRequest(
	@field:Size(max = MAX_DESCRIPTION)
	val description: String? = null,
)

data class ChangeAvailabilityRequest(
	val availability: Availability,
)

// "Visible in" on My copy: the groups that can see the copy. Empty = only the owner sees it.
data class ChangeGroupsRequest(
	val groupIds: Set<UUID>,
)
