package com.bookworms.server.users

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
	fun findByEmail(email: String): User?
	fun existsByEmail(email: String): Boolean
	fun existsByUsername(username: String): Boolean

	@Query(
		"""
		select u from User u
		where lower(u.username) like lower(concat('%', :query, '%'))
		order by u.username asc
		""",
	)
	fun search(query: String): List<User>
}