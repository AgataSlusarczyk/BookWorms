package com.bookworms.server.admin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

// Who did what and when, among the system admins. Written by AdminService with every change it makes.
@Entity
@Table(name = "admin_logs")
class AdminLog(
	val adminId: UUID,
	@field:Enumerated(EnumType.STRING)
	val actionType: AdminAction,
	@field:Enumerated(EnumType.STRING)
	val targetType: AdminTarget,
	val targetId: UUID,
	// What it was, in words – e.g. the book's title, which is gone once the book is deleted.
	@field:Column(columnDefinition = "text")
	val description: String?,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null

	val createdAt: Instant = Instant.now()
}

// The table has no CHECK on it, so a new admin tool only needs a new value here.
enum class AdminAction { BLOCK_USER, UNBLOCK_USER, EDIT_BOOK, DELETE_BOOK, HIDE_REVIEW, DELETE_GROUP }

// The same values as the table's CHECK.
enum class AdminTarget { USER, GROUP, BOOK, REVIEW, COMMENT }

interface AdminLogRepository : JpaRepository<AdminLog, UUID> {
	// The newest first. Older ones stay in the database, the panel just doesn't show them.
	fun findTop100ByOrderByCreatedAtDesc(): List<AdminLog>
}
