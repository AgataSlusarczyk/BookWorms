package com.bookworms.server.books

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.never
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// Open Library and Google Books are faked here, so the test doesn't need the internet.
class CoverDownloaderTest {

	@TempDir
	lateinit var dir: Path

	private lateinit var server: MockRestServiceServer
	private lateinit var downloader: CoverDownloader

	private val isbn = "9788308075197"
	private val image = ByteArray(2_000) { it.toByte() }

	private fun setUp(googleBooksApiKey: String = "") {
		val builder = RestClient.builder()
		server = MockRestServiceServer.bindTo(builder).build()
		downloader = CoverDownloader(builder, CoverProperties(dir, googleBooksApiKey))
	}

	@BeforeEach
	fun withoutGoogle() = setUp()

	@Test
	fun `takes the cover of the edition and saves it`() {
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json"))
			.andRespond(withSuccess("""{"title": "Solaris", "covers": [13314708]}""", MediaType.APPLICATION_JSON))
		server.expect(requestTo("https://covers.openlibrary.org/b/id/13314708-L.jpg?default=false"))
			.andRespond(withSuccess(image, MediaType.IMAGE_JPEG))

		assertEquals("/covers/$isbn.jpg", downloader.download(isbn))
		assertContentEquals(image, Files.readAllBytes(dir.resolve("$isbn.jpg")))
		server.verify()
	}

	@Test
	fun `edition without a cover - tries the covers by ISBN`() {
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json"))
			.andRespond(withSuccess("""{"title": "Bieguni"}""", MediaType.APPLICATION_JSON))
		server.expect(requestTo("https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"))
			.andRespond(withSuccess(image, MediaType.IMAGE_JPEG))

		assertEquals("/covers/$isbn.jpg", downloader.download(isbn))
		server.verify()
	}

	@Test
	fun `no cover anywhere - null and no file`() {
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json")).andRespond(withStatus(HttpStatus.NOT_FOUND))
		server.expect(requestTo("https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND))

		assertNull(downloader.download(isbn))
		assertFalse(Files.exists(dir.resolve("$isbn.jpg")))
		server.verify()
	}

	@Test
	fun `with an API key, Google Books is the last try`() {
		setUp(googleBooksApiKey = "test-key")
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json")).andRespond(withStatus(HttpStatus.NOT_FOUND))
		server.expect(requestTo("https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND))
		server.expect(requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=test-key"))
			.andRespond(
				withSuccess(
					"""{"items": [{"volumeInfo": {"imageLinks": {"thumbnail": "http://books.google.com/cover?id=x"}}}]}""",
					MediaType.APPLICATION_JSON,
				),
			)
		server.expect(requestTo("https://books.google.com/cover?id=x")).andRespond(withSuccess(image, MediaType.IMAGE_JPEG))

		assertEquals("/covers/$isbn.jpg", downloader.download(isbn))
		server.verify()
	}

	@Test
	fun `a tiny placeholder picture doesn't count as a cover`() {
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json"))
			.andRespond(withSuccess("""{"covers": [1]}""", MediaType.APPLICATION_JSON))
		server.expect(requestTo("https://covers.openlibrary.org/b/id/1-L.jpg?default=false"))
			.andRespond(withSuccess(ByteArray(43), MediaType.IMAGE_GIF))
		server.expect(requestTo("https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND))

		assertNull(downloader.download(isbn))
		server.verify()
	}

	@Test
	fun `a cover already saved isn't downloaded again`() {
		Files.write(dir.resolve("$isbn.jpg"), image)
		server.expect(never(), requestTo("https://openlibrary.org/isbn/$isbn.json"))

		assertEquals("/covers/$isbn.jpg", downloader.download(isbn))
		server.verify()
	}

	// ---- Book details (fetchBook) ----

	private fun openLibraryData(json: String) =
		server.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data"))
			.andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

	@Test
	fun `Open Library - all the details, every author and the description of the work`() {
		openLibraryData(
			"""{"ISBN:$isbn": {"title": " Dobry omen ", "authors": [{"name": "Terry Pratchett"}, {"name": "Neil Gaiman"}],
			"publishers": [{"name": "Prószyński"}], "publish_date": "May 2012", "number_of_pages": 352,
			"identifiers": {"openlibrary": ["OL123M"]}, "key": "/books/OL123M"}}""",
		)
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json"))
			.andRespond(withSuccess("""{"works": [{"key": "/works/OL9W"}]}""", MediaType.APPLICATION_JSON))
		server.expect(requestTo("https://openlibrary.org/works/OL9W.json"))
			.andRespond(withSuccess("""{"description": {"type": "/type/text", "value": "The end of the world."}}""", MediaType.APPLICATION_JSON))

		val book = downloader.fetchBook(isbn)!!
		assertEquals(BookSource.OPEN_LIBRARY, book.source)
		assertEquals("OL123M", book.openLibraryId)
		assertEquals("Dobry omen", book.title)
		assertEquals("Terry Pratchett, Neil Gaiman", book.authors)
		assertEquals("Prószyński", book.publisher)
		assertEquals(2012.toShort(), book.publishedYear)
		assertEquals(352, book.pageCount)
		assertEquals("The end of the world.", book.description)
		server.verify()
	}

	@Test
	fun `Open Library without its id - the id is taken from the key`() {
		openLibraryData("""{"ISBN:$isbn": {"title": "Solaris", "authors": [{"name": "Stanisław Lem"}], "key": "/books/OL77M"}}""")
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json")).andRespond(withStatus(HttpStatus.NOT_FOUND))

		val book = downloader.fetchBook(isbn)!!
		assertEquals("OL77M", book.openLibraryId)
		assertNull(book.description)
		server.verify()
	}

	@Test
	fun `without an API key, an unknown book isn't asked from Google`() {
		openLibraryData("{}")
		server.expect(never(), requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn"))

		assertNull(downloader.fetchBook(isbn))
		server.verify()
	}

	@Test
	fun `with an API key, Google fills in the gaps and gives the description`() {
		setUp(googleBooksApiKey = "test-key")
		openLibraryData("""{"ISBN:$isbn": {"title": "Solaris", "authors": [{"name": "Stanisław Lem"}], "key": "/books/OL77M"}}""")
		server.expect(requestTo("https://openlibrary.org/isbn/$isbn.json"))
			.andRespond(withSuccess("""{"description": "Short English text."}""", MediaType.APPLICATION_JSON))
		server.expect(requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=test-key"))
			.andRespond(withSuccess(googleItem(isbn), MediaType.APPLICATION_JSON))

		val book = downloader.fetchBook(isbn)!!
		// Open Library found it, so it stays the source, with its title.
		assertEquals(BookSource.OPEN_LIBRARY, book.source)
		assertEquals("Solaris", book.title)
		assertEquals("Opis po polsku.", book.description)
		assertEquals(320, book.pageCount)
		assertEquals("Wydawnictwo Literackie", book.publisher)
		server.verify()
	}

	@Test
	fun `only Google knows it - a Google book with its id`() {
		setUp(googleBooksApiKey = "test-key")
		openLibraryData("{}")
		server.expect(requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=test-key"))
			.andRespond(withSuccess(googleItem(isbn), MediaType.APPLICATION_JSON))

		val book = downloader.fetchBook(isbn)!!
		assertEquals(BookSource.GOOGLE_BOOKS, book.source)
		assertEquals("g1", book.googleBooksId)
		assertEquals("Solaris (Google)", book.title)
		assertEquals("Stanisław Lem, Ktoś Inny", book.authors)
		server.verify()
	}

	@Test
	fun `Google's answer about another ISBN doesn't count`() {
		setUp(googleBooksApiKey = "test-key")
		openLibraryData("{}")
		server.expect(requestTo("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=test-key"))
			.andRespond(withSuccess(googleItem("9780000000001"), MediaType.APPLICATION_JSON))

		assertNull(downloader.fetchBook(isbn))
		server.verify()
	}

	private fun googleItem(itemIsbn: String) =
		"""{"items": [{"id": "g1", "volumeInfo": {"title": "Solaris (Google)", "authors": ["Stanisław Lem", "Ktoś Inny"],
		"publisher": "Wydawnictwo Literackie", "publishedDate": "2021-03-01", "pageCount": 320, "description": "Opis po polsku.",
		"industryIdentifiers": [{"type": "ISBN_13", "identifier": "$itemIsbn"}]}}]}"""
}
