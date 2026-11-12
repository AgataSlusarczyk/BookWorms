package com.bookworms.server.notifications

import com.bookworms.server.copies.BookCopyRepository
import com.bookworms.server.loans.BorrowRequestRepository
import com.bookworms.server.loans.BorrowingRepository
import com.bookworms.server.loans.QueueRepository
import com.bookworms.server.loans.dto.PersonResponse
import com.bookworms.server.notifications.NotificationType.BORROW_REQUESTED
import com.bookworms.server.notifications.NotificationType.COPY_AVAILABLE
import com.bookworms.server.notifications.NotificationType.HANDOVER_CONFIRMED
import com.bookworms.server.notifications.NotificationType.REQUEST_ACCEPTED
import com.bookworms.server.notifications.NotificationType.REQUEST_CANCELED
import com.bookworms.server.notifications.NotificationType.REQUEST_REJECTED
import com.bookworms.server.notifications.NotificationType.REQUEST_SUPERSEDED
import com.bookworms.server.notifications.NotificationType.RETURN_CONFIRMED
import com.bookworms.server.notifications.NotificationType.RETURN_DUE
import com.bookworms.server.notifications.NotificationType.RETURN_OVERDUE
import com.bookworms.server.notifications.dto.NotificationResponse
import com.bookworms.server.users.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// Makes notifications when something happens to a request, a loan or a queue, and lists them.
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val requestRepository: BorrowRequestRepository,
    private val borrowingRepository: BorrowingRepository,
    private val copyRepository: BookCopyRepository,
    private val queueRepository: QueueRepository,
    private val userRepository: UserRepository,
) {

    // Called inside the change that caused it, so both are saved together – or neither.
    fun notify(userId: UUID, type: NotificationType, referenceId: UUID) {
        notificationRepository.save(Notification(userId = userId, type = type, referenceId = referenceId))
    }

    // The copy can be borrowed again: everyone in its queue is told, and the queue is cleared.
    fun releaseQueue(copyId: UUID) {
        queueRepository.findById_CopyId(copyId).forEach { notify(it.id.userId, COPY_AVAILABLE, copyId) }
        queueRepository.deleteById_CopyId(copyId)
    }

    @Transactional(readOnly = true)
    fun myNotifications(userId: UUID): List<NotificationResponse> = describe(notificationRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId))

    @Transactional
    fun markAllRead(userId: UUID) {
        notificationRepository.markAllRead(userId)
    }

    // Adds the book's title and the other person's name. Everything is loaded at once, not one by one.
    private fun describe(notifications: List<Notification>): List<NotificationResponse> {
        fun idsOf(vararg types: NotificationType) = notifications.filter { it.type in types }.map { it.referenceId }

        val requests = requestRepository.findWithCopyByIdIn(
            idsOf(BORROW_REQUESTED, REQUEST_ACCEPTED, REQUEST_REJECTED, REQUEST_SUPERSEDED, REQUEST_CANCELED),
        ).associateBy { it.id!! }
        val borrowings = borrowingRepository.findWithCopyByIdIn(
            idsOf(HANDOVER_CONFIRMED, RETURN_CONFIRMED, RETURN_DUE, RETURN_OVERDUE),
        ).associateBy { it.id!! }
        val copies = copyRepository.findWithBookByIdIn(idsOf(COPY_AVAILABLE)).associateBy { it.id!! }

        // For each one: the book's title and who the other person is.
        val details = notifications.associateWith { notification ->
            val id = notification.referenceId
            when (notification.type) {
                // Sent to the owner – the other person is the one who asked.
                BORROW_REQUESTED, REQUEST_CANCELED -> requests[id]?.let { it.copy.book.title to it.requesterId }
                // Sent to the one who asked – the other person is the owner.
                REQUEST_ACCEPTED, REQUEST_REJECTED, REQUEST_SUPERSEDED -> requests[id]?.let { it.copy.book.title to it.copy.ownerId }
                // Sent to the one who borrowed – the other person is the owner.
                HANDOVER_CONFIRMED, RETURN_CONFIRMED, RETURN_DUE, RETURN_OVERDUE -> borrowings[id]?.let { it.copy.book.title to it.copy.ownerId }

                COPY_AVAILABLE -> copies[id]?.let { it.book.title to it.ownerId }
            }
        }
        val names = userRepository.findAllById(details.values.mapNotNull { it?.second }.toSet()).associate { it.id!! to it.name }

        return notifications.mapNotNull { notification ->
            val (title, personId) = details[notification] ?: return@mapNotNull null
            NotificationResponse(
                id = notification.id!!,
                type = notification.type,
                referenceId = notification.referenceId,
                read = notification.isRead,
                createdAt = notification.createdAt,
                bookTitle = title,
                person = PersonResponse(personId, names[personId].orEmpty()),
            )
        }
    }
}
