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

	// Looks up a book's details by ISBN (ISBN-13). Open Library first (no key needed), then Google Books (only with a key).
	// When both know the book, Google fills in what Open Library left empty, and its description wins –
	// Google has descriptions of many more books, also in Polish.
	fun fetchBook(isbn: String): Book? {
		val openLibrary = openLibraryBook(isbn)
		val google = if (properties.googleBooksApiKey.isNotBlank()) googleBooksBook(isbn) else null
		val book = openLibrary ?: return google
		if (google != null) {
			book.description = google.description ?: book.description
			book.pageCount = book.pageCount ?: google.pageCount
			book.publisher = book.publisher ?: google.publisher
			book.publishedYear = book.publishedYear ?: google.publishedYear
		}
		return book
	}

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
		if (properties.googleBooksApiKey.isBlank()) return null
		val result = googleBooksJson(isbn) ?: return null
		val links = result.path("items").firstOrNull()?.path("volumeInfo")?.path("imageLinks") ?: return null
		// The biggest picture Google gives without asking twice. It sends http:// links, but https works too.
		val url = listOf("large", "medium", "thumbnail").map { links.path(it).asString("") }.firstOrNull { it.isNotEmpty() }
		return url?.replaceFirst("http://", "https://")
	}

	private fun openLibraryBook(isbn: String): Book? {
		val root = getJson("https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data") ?: return null
		val node = root.path("ISBN:$isbn")
		val title = text(node.path("title"))
		val authors = node.path("authors").mapNotNull { text(it.path("name")) }.joinToString(", ")
		// The database needs Open Library's own id of a book that came from there, e.g. "OL7353617M".
		val openLibraryId = text(node.path("identifiers").path("openlibrary").firstOrNull())
			?: text(node.path("key"))?.substringAfterLast('/')
		if (title == null || authors.isEmpty() || openLibraryId == null) return null

		return Book(
			source = BookSource.OPEN_LIBRARY,
			title = title.take(MAX_TITLE),
			authors = authors.take(MAX_AUTHORS),
			isbn = isbn,
			publisher = text(node.path("publishers").firstOrNull()?.path("name"))?.take(MAX_PUBLISHER),
			publishedYear = parseYear(node.path("publish_date").asString("")),
			pageCount = node.path("number_of_pages").asInt(0).takeIf { it > 0 },
			description = openLibraryDescription(isbn),
			openLibraryId = openLibraryId,
		)
	}

	// The edition's own description, or else the one of the work (the same book in all its editions).
	private fun openLibraryDescription(isbn: String): String? {
		val edition = getJson("https://openlibrary.org/isbn/$isbn.json") ?: return null
		descriptionText(edition.path("description"))?.let { return it }
		val work = text(edition.path("works").firstOrNull()?.path("key")) ?: return null
		return descriptionText(getJson("https://openlibrary.org$work.json")?.path("description"))
	}

	// Open Library keeps a description either as plain text or as {"type": "/type/text", "value": "..."}.
	private fun descriptionText(node: JsonNode?): String? =
		if (node != null && node.isObject) text(node.path("value")) else text(node)

	private fun googleBooksBook(isbn: String): Book? {
		val item = googleBooksJson(isbn)?.path("items")?.firstOrNull() ?: return null
		val info = item.path("volumeInfo")
		// Google sometimes answers with a similar book – only exactly this ISBN counts.
		if (info.path("industryIdentifiers").none { it.path("identifier").asString("") == isbn }) return null
		val title = text(info.path("title"))
		val authors = info.path("authors").mapNotNull { text(it) }.joinToString(", ")
		// The database needs Google's own id of a book that came from there.
		val googleBooksId = text(item.path("id"))
		if (title == null || authors.isEmpty() || googleBooksId == null) return null

		return Book(
			source = BookSource.GOOGLE_BOOKS,
			title = title.take(MAX_TITLE),
			authors = authors.take(MAX_AUTHORS),
			isbn = isbn,
			publisher = text(info.path("publisher"))?.take(MAX_PUBLISHER),
			publishedYear = parseYear(info.path("publishedDate").asString("")),
			pageCount = info.path("pageCount").asInt(0).takeIf { it > 0 },
			description = text(info.path("description")),
			googleBooksId = googleBooksId,
		)
	}

	// The text in the node without spaces at the ends. null when there's no node or it's empty.
	private fun text(node: JsonNode?): String? =
		node?.asString("")?.trim()?.ifEmpty { null }

	private fun googleBooksJson(isbn: String): JsonNode? {
		val keyParam = if (properties.googleBooksApiKey.isNotBlank()) "&key=${properties.googleBooksApiKey}" else ""
		return getJson("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn$keyParam")
	}

	private fun parseYear(date: String): Short? =
		YEAR_REGEX.find(date)?.value?.toShortOrNull()

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
		// The longest texts the books table takes.
		const val MAX_TITLE = 500
		const val MAX_AUTHORS = 1000
		const val MAX_PUBLISHER = 255
		val YEAR_REGEX = Regex("""\b\d{4}\b""")
	}
}