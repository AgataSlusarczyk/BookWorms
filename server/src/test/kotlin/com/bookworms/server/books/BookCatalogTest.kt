package com.bookworms.server.books

import com.bookworms.server.TestcontainersConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.web.client.RestClient
import java.nio.file.Path
import org.springframework.test.web.servlet.get
import kotlin.test.assertNotNull

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class BookCatalogTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val bookRepository: BookRepository,
	@Autowired private val genreRepository: GenreRepository,
) {

	// The fake from coverDownloader() below, so the tests don't ask the real Open Library and Google Books.
	@TestBean
	lateinit var coverDownloader: CoverDownloader

	@BeforeEach
	fun saveBooks() {
		online.clear()
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
	fun `a book found online is saved in the catalog, so it's asked only once`() {
		online["9788381351973"] = Book(
			BookSource.OPEN_LIBRARY, "Reminders of him", "Colleen Hoover", "9788381351973",
			description = "Opis.", openLibraryId = "OL1M",
		)

		mockMvc.get("/books/isbn/978-83-8135-197-3").andExpect {
			status { isOk() }
			jsonPath("$.title") { value("Reminders of him") }
			jsonPath("$.description") { value("Opis.") }
			jsonPath("$.coverUrl") { value("/covers/9788381351973.jpg") }
		}
		assertNotNull(bookRepository.findByIsbn("9788381351973"))
	}

	@Test
	fun `an old ISBN-10 finds the book saved with its ISBN-13`() {
		bookRepository.save(Book(BookSource.MANUAL, "My Book", "Me", "9780306406157"))

		mockMvc.get("/books/isbn/0-306-40615-2").andExpect {
			status { isOk() }
			jsonPath("$.title") { value("My Book") }
		}
	}

	@Test
	fun `unknown ISBN returns 404`() {
		mockMvc.get("/books/isbn/9788383355702").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("BOOK_NOT_FOUND") }
		}
	}

	companion object {
		// The books "on the internet" – each test puts in what it needs.
		private val online = mutableMapOf<String, Book>()

		// Spring calls it for the @TestBean above (same name).
		@JvmStatic
		fun coverDownloader(): CoverDownloader =
			object : CoverDownloader(RestClient.builder(), CoverProperties(Path.of("build/test-covers"))) {
				override fun fetchBook(isbn: String): Book? = online[isbn]

				override fun download(isbn: String): String? = if (isbn in online) "/covers/$isbn.jpg" else null
			}
	}
}
