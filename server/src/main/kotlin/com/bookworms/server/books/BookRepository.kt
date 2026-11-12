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

	// Loads the genres too, because the details show them.
	@EntityGraph(attributePaths = ["genres"])
	fun findByIsbn(isbn: String): Book?
}

interface GenreRepository : JpaRepository<Genre, UUID> {
	fun findByCode(code: String): Genre?
}
