package com.bookworms.server.books

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

// One of the fixed genres from the migration, e.g. FANTASY. The app turns the code into a name.
@Entity
@Table(name = "genres")
class Genre(
	@Id
	var id: UUID,
	var code: String,
)
