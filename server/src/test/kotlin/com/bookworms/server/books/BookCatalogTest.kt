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
) {

	@BeforeEach
	fun saveBooks() {
		bookRepository.deleteAll()
		bookRepository.saveAll(
			listOf(
				Book(BookSource.MANUAL, "Solaris", "Stanisław Lem", "9788308049461", publishedYear = 1961),
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
}
