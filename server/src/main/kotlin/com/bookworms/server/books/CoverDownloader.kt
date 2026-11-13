package com.bookworms.server.books

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import tools.jackson.databind.JsonNode
import java.nio.file.Files

// Finds a book's cover by ISBN on the internet and saves it in bookworms.covers.dir.
// Tries, in this order:
//   1. Open Library edition (openlibrary.org/isbn/<isbn>.json) – the cover of exactly that edition,
//   2. Open Library covers by ISBN – also finds covers kept on another copy of the same edition,
//   3. Google Books (googleapis.com/books/v1/volumes?q=isbn:<isbn>) – only with an API key.
// Open Library needs no key. The app never calls these – only the server does, so a key stays here.
@Component
class CoverDownloader(
	restClientBuilder: RestClient.Builder,
	private val properties: CoverProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val http = restClientBuilder
		// Open Library asks clients to say who they are.
		.defaultHeader(HttpHeaders.USER_AGENT, "BookWorms/0.1 (+https://github.com/AgataSlusarczyk/BookWorms)")
		.build()

	// The path to put in books.cover_url, e.g. "/covers/9788308075197.jpg", or null when no cover was found.
	fun download(isbn: String): String? {
		val file = properties.dir.resolve("$isbn.jpg")
		if (Files.exists(file)) {
			return coverPath(isbn)
		}
		val image = imageUrls(isbn).firstNotNullOfOrNull { fetchImage(it) } ?: return null
		Files.createDirectories(properties.dir)
		Files.write(file, image)
		log.info("Saved the cover of {} ({} KB)", isbn, image.size / 1024)
		return coverPath(isbn)
	}

	// Looks up a book's details by ISBN in Open Library first, then Google Books.
	fun fetchBook(isbn: String): Book? =
		openLibraryBook(isbn) ?: googleBooksBook(isbn)

	// Addresses of images to try, the best one first. Each is asked only when the one before had nothing.
	private fun imageUrls(isbn: String): Sequence<String> = sequence {
		openLibraryCoverId(isbn)?.let { yield("https://covers.openlibrary.org/b/id/$it-L.jpg?default=false") }
		// default=false: 404 instead of a blank picture when there's no cover.
		yield("https://covers.openlibrary.org/b/isbn/$isbn-L.jpg?default=false")
		googleBooksImage(isbn)?.let { yield(it) }
	}

	private fun openLibraryCoverId(isbn: String): Long? {
		val edition = getJson("https://openlibrary.org/isbn/$isbn.json") ?: return null
		// Open Library uses -1 for a removed cover.
		return edition.path("covers").firstOrNull { it.asLong() > 0 }?.asLong()
	}

	private fun googleBooksImage(isbn: String): String? {
		val result = googleBooksJson(isbn) ?: return null
		val links = result.path("items").firstOrNull()?.path("volumeInfo")?.path("imageLinks") ?: return null
		// The biggest picture Google gives without asking twice. It sends http:// links, but https works too.
		val url = listOf("large", "medium", "thumbnail").map { links.path(it).asString("") }.firstOrNull { it.isNotEmpty() }
		return url?.replaceFirst("http://", "https://")
	}

	private fun openLibraryBook(isbn: String): Book? {
		val root = getJson("https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data") ?: return null
		val node = root.path("ISBN:$isbn")
		val title = node.path("title").asString("").trim()
		val author = node.path("authors").firstOrNull()?.path("name")?.asString("")?.trim().orEmpty()
		if (title.isEmpty() || author.isEmpty()) return null

		val publisher = node.path("publishers").firstOrNull()?.path("name")?.asString("")?.trim()?.ifEmpty { null }
		val year = parseYear(node.path("publish_date").asString(""))
		return Book(
			isbn = isbn,
			title = title,
			author = author,
			publisher = publisher,
			publishedYear = year,
		)
	}

	private fun googleBooksBook(isbn: String): Book? {
		val result = googleBooksJson(isbn) ?: return null
		val info = result.path("items").firstOrNull()?.path("volumeInfo") ?: return null
		val title = info.path("title").asString("").trim()
		val author = info.path("authors").firstOrNull()?.asString("")?.trim().orEmpty()
		if (title.isEmpty() || author.isEmpty()) return null

		val publisher = info.path("publisher").asString("").trim().ifEmpty { null }
		val year = parseYear(info.path("publishedDate").asString(""))
		return Book(
			isbn = isbn,
			title = title,
			author = author,
			publisher = publisher,
			publishedYear = year,
		)
	}

	private fun googleBooksJson(isbn: String): JsonNode? {
		val keyParam = if (properties.googleBooksApiKey.isNotBlank()) "&key=${properties.googleBooksApiKey}" else ""
		return getJson("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn$keyParam")
	}

	private fun parseYear(date: String): Int? =
		YEAR_REGEX.find(date)?.value?.toIntOrNull()

	private fun getJson(url: String): JsonNode? =
		try {
			http.get().uri(url).retrieve().body<JsonNode>()
		} catch (e: RestClientException) {
			// 404 = they don't know this ISBN. Anything else (no internet, too many requests) – also just skip it.
			log.debug("No answer from {}: {}", url.substringBefore("&key="), e.message)
			null
		}

	private fun fetchImage(url: String): ByteArray? =
		try {
			val response = http.get().uri(url).retrieve().toEntity(ByteArray::class.java)
			val isImage = response.headers.contentType?.type == "image"
			// A few hundred bytes is a placeholder, not a cover.
			response.body?.takeIf { isImage && it.size > MIN_IMAGE_BYTES }
		} catch (e: RestClientException) {
			log.debug("No image at {}: {}", url.substringBefore("&key="), e.message)
			null
		}

	private fun coverPath(isbn: String) = "/covers/$isbn.jpg"

	private companion object {
		const val MIN_IMAGE_BYTES = 1_000
		val YEAR_REGEX = Regex("""\b\d{4}\b""")
	}
}