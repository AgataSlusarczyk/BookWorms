package com.bookworms.server.admin

import com.bookworms.server.admin.dto.AdminGroupResponse
import com.bookworms.server.admin.dto.AdminLogResponse
import com.bookworms.server.admin.dto.AdminStatsResponse
import com.bookworms.server.admin.dto.AdminUserResponse
import com.bookworms.server.books.Book
import com.bookworms.server.books.BookNotFoundException
import com.bookworms.server.books.BookRepository
import com.bookworms.server.books.dto.BookDetailsResponse
import com.bookworms.server.books.dto.toDetailsResponse
import com.bookworms.server.common.ApiException
import com.bookworms.server.copies.dto.EditBookRequest
import com.bookworms.server.groups.GroupMemberRepository
import com.bookworms.server.groups.GroupRepository
import com.bookworms.server.groups.GroupService
import com.bookworms.server.reviews.BookReviewRepository
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
import com.bookworms.server.users.UserStatus
import com.bookworms.server.users.people
import com.bookworms.server.users.person
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

// What only a system admin (users.global_role = SYSTEM_ADMIN) can do: the admin panel, and more on the other screens –
// edit and delete any book, hide reviews, block people, delete any group.
// Every method first checks who asks; everyone else gets 403. Every change is written to admin_logs.
@Service
class AdminService(
	private val userRepository: UserRepository,
	private val bookRepository: BookRepository,
	private val reviewRepository: BookReviewRepository,
	private val groupRepository: GroupRepository,
	private val memberRepository: GroupMemberRepository,
	private val groupService: GroupService,
	private val logRepository: AdminLogRepository,
	private val jdbc: JdbcTemplate,
) {

	@Transactional(readOnly = true)
	fun stats(adminId: UUID): AdminStatsResponse {
		requireAdmin(adminId)
		fun count(sql: String) = jdbc.queryForObject(sql, Long::class.java) ?: 0
		return AdminStatsResponse(
			users = userRepository.count(),
			blockedUsers = userRepository.countByStatus(UserStatus.BLOCKED),
			books = bookRepository.count(),
			copies = count("select count(*) from book_copies where deleted_at is null"),
			groups = groupRepository.count(),
			reviews = count("select count(*) from book_reviews where not is_hidden"),
			activeLoans = count("select count(*) from borrowings where status <> 'RETURNED'"),
		)
	}

	// No query = the newest accounts. Otherwise those whose name, username or email has the text in it.
	@Transactional(readOnly = true)
	fun users(adminId: UUID, query: String?): List<AdminUserResponse> {
		requireAdmin(adminId)
		val escaped = query?.trim().orEmpty().lowercase()
			.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
		return userRepository.searchForAdmin("%$escaped%", PageRequest.of(0, LIST_LIMIT)).map { it.toAdminResponse() }
	}

	// The user can't log in anymore, and the token they have stops working (see BlockedUserFilter).
	// Not yourself (nobody would be left to unblock you) and not another admin.
	@Transactional
	fun block(adminId: UUID, userId: UUID): AdminUserResponse {
		requireAdmin(adminId)
		val user = user(userId)
		if (userId == adminId) {
			throw ApiException(HttpStatus.CONFLICT, "CANNOT_BLOCK_SELF", "You can't block yourself")
		}
		if (user.isAdmin) {
			throw ApiException(HttpStatus.CONFLICT, "CANNOT_BLOCK_ADMIN", "An admin can't be blocked")
		}
		if (user.status != UserStatus.BLOCKED) {
			user.status = UserStatus.BLOCKED
			log(adminId, AdminAction.BLOCK_USER, AdminTarget.USER, userId, "@${user.username}")
		}
		return user.toAdminResponse()
	}

	@Transactional
	fun unblock(adminId: UUID, userId: UUID): AdminUserResponse {
		requireAdmin(adminId)
		val user = user(userId)
		if (user.status != UserStatus.ACTIVE) {
			user.status = UserStatus.ACTIVE
			log(adminId, AdminAction.UNBLOCK_USER, AdminTarget.USER, userId, "@${user.username}")
		}
		return user.toAdminResponse()
	}

	// Every group, the newest first – also the ones the admin isn't in.
	@Transactional(readOnly = true)
	fun groups(adminId: UUID): List<AdminGroupResponse> {
		requireAdmin(adminId)
		val groups = groupRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
		if (groups.isEmpty()) return emptyList()
		val counts = memberRepository.countMembers(groups.map { it.id!! })
			.associate { (id, count) -> id as UUID to count as Long }
		val people = userRepository.people(groups.map { it.createdBy })
		return groups.map {
			AdminGroupResponse(
				id = it.id!!,
				name = it.name,
				description = it.description,
				photoUrl = it.photoUrl,
				memberCount = counts[it.id] ?: 0,
				createdBy = people.person(it.createdBy),
				createdAt = it.createdAt,
			)
		}
	}

	// Its members, chat and the copies shown in it go with it, like when its own admin deletes it.
	@Transactional
	fun deleteGroup(adminId: UUID, groupId: UUID) {
		requireAdmin(adminId)
		val group = groupRepository.findByIdOrNull(groupId)
			?: throw ApiException(HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND", "Group not found")
		val name = group.name
		groupService.deleteBySystemAdmin(groupId)
		log(adminId, AdminAction.DELETE_GROUP, AdminTarget.GROUP, groupId, name)
	}

	// Any book in the catalog, also one from Google Books or Open Library – e.g. to fix a typo for everyone.
	// The ISBN stays: the app finds books by it, and the cover file is named after it.
	@Transactional
	fun editBook(adminId: UUID, bookId: UUID, request: EditBookRequest): BookDetailsResponse {
		requireAdmin(adminId)
		val book = book(bookId)
		book.title = request.title.trim()
		book.authors = request.authors.trim()
		book.publisher = request.publisher?.trim()?.ifEmpty { null }
		book.publishedYear = request.publishedYear?.toShort()
		book.description = request.description?.trim()?.ifEmpty { null }
		// Phones keep their copies of these books and ask for what changed after their last sync.
		jdbc.update("update book_copies set updated_at = now() where book_id = ?", bookId)
		log(adminId, AdminAction.EDIT_BOOK, AdminTarget.BOOK, bookId, book.title)
		return book.toDetailsResponse()
	}

	// The book is gone for good, with everything about it: people's copies, the requests and loans of those copies,
	// ratings, reading statuses, places on lists and group comments. Their notifications go too, since they'd lead nowhere.
	// The tables keep the book from being deleted while any of these point to it (ON DELETE RESTRICT), so they go first.
	@Transactional
	fun deleteBook(adminId: UUID, bookId: UUID) {
		requireAdmin(adminId)
		val book = book(bookId)
		val title = book.title
		val copies = "select id from book_copies where book_id = ?"
		val requests = "select id from borrow_requests where copy_id in ($copies)"
		jdbc.update(
			"""
			delete from notifications where reference_id in ($copies)
			   or reference_id in ($requests)
			   or reference_id in (select id from borrowings where copy_id in ($copies))
			""",
			bookId, bookId, bookId,
		)
		jdbc.update("delete from borrowings where copy_id in ($copies)", bookId)
		jdbc.update("delete from borrow_requests where copy_id in ($copies)", bookId)
		// Their places in groups and queues go with them (ON DELETE CASCADE).
		jdbc.update("delete from book_copies where book_id = ?", bookId)
		jdbc.update("delete from book_reviews where book_id = ?", bookId)
		jdbc.update("delete from user_book_status where book_id = ?", bookId)
		// The lists stay, without this book. Their updatedAt moves, so phones see the change.
		jdbc.update(
			"update book_lists set updated_at = now() where id in (select list_id from book_list_items where book_id = ?)",
			bookId,
		)
		jdbc.update("delete from book_list_items where book_id = ?", bookId)
		jdbc.update("delete from group_book_comments where book_id = ?", bookId)
		// Its genres go with it (ON DELETE CASCADE). The cover file stays – it's named after the ISBN,
		// so the same book added again gets it back without downloading.
		bookRepository.delete(book)
		bookRepository.flush()
		log(adminId, AdminAction.DELETE_BOOK, AdminTarget.BOOK, bookId, title)
	}

	// A review that breaks the rules. It stays in the database, but nobody sees it and it doesn't count in the average.
	@Transactional
	fun hideReview(adminId: UUID, reviewId: UUID) {
		requireAdmin(adminId)
		val review = reviewRepository.findByIdOrNull(reviewId)
			?: throw ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Review not found")
		if (review.isHidden) return
		review.isHidden = true
		val author = userRepository.findByIdOrNull(review.userId)?.username
		val book = bookRepository.findByIdOrNull(review.bookId)?.title
		log(adminId, AdminAction.HIDE_REVIEW, AdminTarget.REVIEW, reviewId, listOfNotNull(author?.let { "@$it" }, book).joinToString(" · "))
	}

	@Transactional(readOnly = true)
	fun logs(adminId: UUID): List<AdminLogResponse> {
		requireAdmin(adminId)
		val logs = logRepository.findTop100ByOrderByCreatedAtDesc()
		val admins = userRepository.people(logs.map { it.adminId })
		return logs.map {
			AdminLogResponse(
				id = it.id!!,
				admin = admins.person(it.adminId),
				action = it.actionType,
				targetType = it.targetType,
				targetId = it.targetId,
				description = it.description,
				createdAt = it.createdAt,
			)
		}
	}

	// Checked in the database on every request, not from the token: taking the role away works at once.
	private fun requireAdmin(userId: UUID) {
		if (userRepository.findByIdOrNull(userId)?.isAdmin != true) {
			throw ApiException(HttpStatus.FORBIDDEN, "NOT_SYSTEM_ADMIN", "Only a system admin can do this")
		}
	}

	private fun log(adminId: UUID, action: AdminAction, target: AdminTarget, targetId: UUID, description: String?) {
		logRepository.save(AdminLog(adminId, action, target, targetId, description))
	}

	private fun user(userId: UUID): User =
		userRepository.findByIdOrNull(userId)
			?: throw ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found")

	private fun book(bookId: UUID): Book =
		bookRepository.findByIdOrNull(bookId) ?: throw BookNotFoundException()

	private fun User.toAdminResponse() = AdminUserResponse(
		id = id!!,
		name = name,
		username = username,
		email = email,
		photoUrl = photoUrl,
		role = globalRole,
		status = status,
		createdAt = createdAt,
	)

	private companion object {
		const val LIST_LIMIT = 100
	}
}
