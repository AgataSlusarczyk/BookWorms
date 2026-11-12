package com.bookworms.server.books

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import tools.jackson.databind.JsonNode

// 1. Model danych zwracany do aplikacji mobilnej
data class ScannedBookDto(
    val isbn: String,
    val title: String,
    val author: String,
    val year: Int
)

// 2. Serwis odpytujący zewnętrzne API (logika identyczna jak w CoverDownloader)
@Component
class BookMetadataFetcher(
    restClientBuilder: RestClient.Builder,
    private val properties: CoverProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = restClientBuilder
        .defaultHeader(HttpHeaders.USER_AGENT, "BookWorms/0.1 (+https://github.com/AgataSlusarczyk/BookWorms)")
        .build()

    fun fetchDetails(isbn: String): ScannedBookDto? {
        return getFromOpenLibrary(isbn) ?: getFromGoogleBooks(isbn)
    }

    private fun getFromOpenLibrary(isbn: String): ScannedBookDto? {
        val url = "https://openlibrary.org/isbn/$isbn.json"
        val node = getJson(url) ?: return null
        
        val title = node.path("title").asText("")
        val author = node.path("by_statement").asText("Unknown Author")
        val publishDate = node.path("publish_date").asText("")
        val year = Regex("\\d{4}").find(publishDate)?.value?.toIntOrNull() ?: 0
        
        if (title.isBlank()) return null
        log.info("Fetched metadata for {} from Open Library", isbn)
        return ScannedBookDto(isbn, title, author, year)
    }

    private fun getFromGoogleBooks(isbn: String): ScannedBookDto? {
        val key = properties.googleBooksApiKey
        // Zgodnie z założeniem z properties - jeśli klucz istnieje, używamy go, aby uniknąć limitów
        val url = if (key.isNotBlank()) {
            "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=$key"
        } else {
            "https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn"
        }
        
        val node = getJson(url) ?: return null
        val volumeInfo = node.path("items").firstOrNull()?.path("volumeInfo") ?: return null
        
        val title = volumeInfo.path("title").asText("")
        val author = volumeInfo.path("authors").firstOrNull()?.asText() ?: "Unknown Author"
        val publishDate = volumeInfo.path("publishedDate").asText("")
        val year = Regex("\\d{4}").find(publishDate)?.value?.toIntOrNull() ?: 0
        
        if (title.isBlank()) return null
        log.info("Fetched metadata for {} from Google Books", isbn)
        return ScannedBookDto(isbn, title, author, year)
    }

    private fun getJson(url: String): JsonNode? =
        try {
            http.get().uri(url).retrieve().body<JsonNode>()
        } catch (e: RestClientException) {
            log.debug("No answer from {}: {}", url.substringBefore("&key="), e.message)
            null
        }
}

// 3. Kontroler wystawiający endpoint dla aplikacji Android
@RestController
class BookLookupController(
    private val fetcher: BookMetadataFetcher
) {
    @GetMapping("/api/books/lookup")
    fun lookupBook(@RequestParam isbn: String): ScannedBookDto {
        // Jeśli nie znajdzie w API, zwraca wypełniony zastępnik, żeby aplikacja zawsze otrzymała JSON
        return fetcher.fetchDetails(isbn) ?: ScannedBookDto(
            isbn = isbn,
            title = isbn, // Pusty tytuł jako ISBN, aby Android mógł wkleić do prefill
            author = "Unknown",
            year = 0
        )
    }
}