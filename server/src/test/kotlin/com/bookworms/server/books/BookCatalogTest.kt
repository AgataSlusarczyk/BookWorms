package com.bookworms.server.books

import com.bookworms.server.TestcontainersConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class BookCatalogTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val bookRepository: BookRepository,
	@Autowired private val genreRepository: GenreRepository,
) {

	@BeforeEach
	fun saveBooks() {
		bookRepository.deleteAll()
		bookRepository.saveAll(
			listOf(
				Book(
					BookSource.MANUAL, "Solaris", "Stanisław Lem", "9788308049461",
					publisher = "Wydawnictwo Literackie", publishedYear = 1961, pageCount = 224,
					coverUrl = "/covers/9788308049461.jpg",
					description = "Stacja badawcza nad oceanem planety.",
				).apply { genres.add(genreRepository.findByCode("SCI_FI")!!) },
				Book(BookSource.MANUAL, "Niezwyciężony", "Stanisław Lem", "9788308057619", publishedYear = 1964),
				Book(BookSource.MANUAL, "Lalka", "Bolesław Prus", "9788373271897", publishedYear = 1890),
			),
		)
	}

	@Test
	fun `catalog works without logging in`() {
		mockMvc.get("/books").andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(3) }
		}
	}

	@Test
	fun `search finds the author whatever the letter case`() {
		mockMvc.get("/books") { param("query", "LEM") }.andExpect {
			status { isOk() }
			jsonPath("$.length()") { value(2) }
			jsonPath("$[*].title") { value(org.hamcrest.Matchers.containsInAnyOrder("Solaris", "Niezwyciężony")) }
		}
	}

	@Test
	fun `search finds a title with Polish letters`() {
		mockMvc.get("/books") { param("query", "zwycię") }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].title") { value("Niezwyciężony") }
			jsonPath("$[0].publishedYear") { value(1964) }
			jsonPath("$[0].coverUrl") { doesNotExist() }
		}
	}

	@Test
	fun `search finds part of an ISBN typed with dashes`() {
		mockMvc.get("/books") { param("query", "978-83-73") }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].isbn") { value("9788373271897") }
		}
	}

	@Test
	fun `percent sign in the query is a plain character`() {
		mockMvc.get("/books") { param("query", "%") }.andExpect {
			jsonPath("$.length()") { value(0) }
		}
	}

	@Test
	fun `limit cuts the list`() {
		mockMvc.get("/books") { param("limit", "2") }.andExpect {
			jsonPath("$.length()") { value(2) }
		}
	}

	@Test
	fun `details by ISBN, also typed with dashes, without logging in`() {
		mockMvc.get("/books/isbn/978-83-08-04946-1").andExpect {
			status { isOk() }
			jsonPath("$.title") { value("Solaris") }
			jsonPath("$.authors") { value("Stanisław Lem") }
			jsonPath("$.publishedYear") { value(1961) }
			jsonPath("$.pageCount") { value(224) }
			jsonPath("$.description") { value("Stacja badawcza nad oceanem planety.") }
			jsonPath("$.genres[0]") { value("SCI_FI") }
			jsonPath("$.coverUrl") { value("/covers/9788308049461.jpg") }
		}
	}

	@Test
	fun `unknown ISBN returns 404`() {
		mockMvc.get("/books/isbn/9788383355702").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("BOOK_NOT_FOUND") }
		}
	}
}