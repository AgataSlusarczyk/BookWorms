package com.bookworms.server.users

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun existsByEmail(email: String): Boolean
	fun existsByUsername(username: String): Boolean

	// Use searchByUsername, which makes the pattern.
	@Query(
		"""
		select u from User u
		where lower(u.username) like :pattern escape '\'
		order by u.username asc
		""",
	)
	fun findByUsernamePattern(pattern: String): List<User>
}

// Users whose username has this text in it, in any case. % and _ are just letters here, not "anything".
fun UserRepository.searchByUsername(text: String): List<User> {
	val escaped = text.lowercase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
	return findByUsernamePattern("%$escaped%")
}
