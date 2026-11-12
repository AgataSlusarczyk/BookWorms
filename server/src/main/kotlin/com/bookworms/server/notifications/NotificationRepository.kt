package com.bookworms.server.notifications

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, UUID> {

    // The newest first. Older ones aren't sent – nobody scrolls that far.
    fun findTop100ByUserIdOrderByCreatedAtDesc(userId: UUID): List<Notification>

    // One update for all of them, instead of loading each one.
    @Modifying
    @Query("update Notification n set n.isRead = true where n.userId = :userId and n.isRead = false")
    fun markAllRead(userId: UUID)
}
