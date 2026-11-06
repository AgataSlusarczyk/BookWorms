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
}
