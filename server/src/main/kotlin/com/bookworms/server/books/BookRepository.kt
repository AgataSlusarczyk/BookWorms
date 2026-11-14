package com.bookworms.server.books

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface BookRepository : JpaRepository<Book, UUID> {

	@Query("select b from Book b order by b.createdAt desc, b.title")
	fun findNewest(pageable: Pageable): List<Book>

	// pattern is already lower case with % around it. isbnDigits is empty when the query isn't an ISBN.
	@Query(
		"""
		select b from Book b
		where lower(b.title) like :pattern escape '!'
		   or lower(b.authors) like :pattern escape '!'
		   or (:isbnDigits <> '' and b.isbn like concat('%', :isbnDigits, '%'))
		order by b.createdAt desc, b.title
		""",
	)
	fun search(pattern: String, isbnDigits: String, pageable: Pageable): List<Book>

	// Books the server could still look up a cover for.
	fun findByIsbnIsNotNullAndCoverUrlIsNull(): List<Book>

	// The Library: books this user has, or someone has in one of their groups. Each book once,
	// the one with the newest copy first. Removed copies don't count.
	@Query(
		value = """
		select b.* from books b
		join (
			select c.book_id, max(c.created_at) as newest from book_copies c
			where c.deleted_at is null
			  and (c.owner_id = :userId or exists (
				select 1 from copy_group_visibility v
				join group_members m on m.group_id = v.group_id
				where v.copy_id = c.id and m.user_id = :userId
			  ))
			group by c.book_id
		) mine on mine.book_id = b.id
		order by mine.newest desc, b.title
		limit :limit
		""",
		nativeQuery = true,
	)
	fun findInLibraryOf(userId: UUID, limit: Int): List<Book>

	// Loads the genres too, because the details show them.
	@EntityGraph(attributePaths = ["genres"])
	fun findByIsbn(isbn: String): Book?
}

interface GenreRepository : JpaRepository<Genre, UUID> {
	fun findByCode(code: String): Genre?
}
