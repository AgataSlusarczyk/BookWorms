package com.bookworms.server.users

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun existsByEmail(email: String): Boolean
	fun existsByUsername(username: String): Boolean
	fun findByUsername(username: String): User?

	// Use searchByUsername, which makes the pattern.
	@Query(
		"""
		select u from User u
		where lower(u.username) like :pattern escape '\'
		order by u.username asc
		""",
	)
	fun findByUsernamePattern(pattern: String): List<User>

	// Only the status, without loading the whole user – asked on every request (see BlockedUserFilter).
	@Query("select u.status from User u where u.id = :id")
	fun findStatusById(id: UUID): UserStatus?

	fun countByStatus(status: UserStatus): Long

	// The admin panel's list: name, username or email has the text in it. pattern is already lower case with % around it.
	@Query(
		"""
		select u from User u
		where lower(u.name) like :pattern escape '\'
		   or u.username like :pattern escape '\'
		   or u.email like :pattern escape '\'
		order by u.createdAt desc
		""",
	)
	fun searchForAdmin(pattern: String, pageable: Pageable): List<User>
}

// Users whose username has this text in it, in any case. % and _ are just letters here, not "anything".
fun UserRepository.searchByUsername(text: String): List<User> {
	val escaped = text.lowercase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
	return findByUsernamePattern("%$escaped%")
}
