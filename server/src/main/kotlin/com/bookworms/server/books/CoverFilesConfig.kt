package com.bookworms.server.books

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

// Serves cover images at /covers/<file>, e.g. /covers/9788308049461.jpg.
// books.cover_url keeps that path, and the app adds the server address in front of it.
// Where the files are: bookworms.covers.locations (application.yaml, and application-dev.yaml
// adds the sample covers). The first folder that has the file wins.
@Configuration
class CoverFilesConfig(
	@Value("\${bookworms.covers.locations}") private val locations: List<String>,
) : WebMvcConfigurer {

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.addResourceHandler("/covers/**")
			// Every folder must end with "/", or Spring looks for the files next to it.
			.addResourceLocations(*locations.map { if (it.endsWith("/")) it else "$it/" }.toTypedArray())
	}
}
