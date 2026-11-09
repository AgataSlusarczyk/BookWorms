package com.bookworms.server.books

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path

@ConfigurationProperties("bookworms.covers")
data class CoverProperties(
	// The folder with cover images. Relative paths start where the server runs (server/ for bootRun).
	val dir: Path,
	// Empty = don't ask Google Books.
	val googleBooksApiKey: String = "",
)

// Serves the cover images from bookworms.covers.dir at /covers/<file>, e.g. /covers/9788308075197.jpg.
// books.cover_url keeps that path, and the app adds the server address in front of it.
@Configuration
@EnableConfigurationProperties(CoverProperties::class)
class CoverFilesConfig(
	private val properties: CoverProperties,
) : WebMvcConfigurer {

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		// The folder must end with "/", or Spring looks for the files next to it.
		registry.addResourceHandler("/covers/**")
			.addResourceLocations(properties.dir.toAbsolutePath().toUri().toString().trimEnd('/') + "/")
	}
}
