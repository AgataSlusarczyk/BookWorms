package com.bookworms.server.dev

import com.bookworms.server.books.CoverService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import javax.sql.DataSource
import kotlin.concurrent.thread

// Sample data for local development, so the app has something to show.
// Only with the dev profile (./gradlew bootRun turns it on) – never on a real server, never in tests.
// Runs on every start; the books in dev-data/books.sql win over what's in the database.
@Component
@Profile("dev")
class DevDataLoader(
	private val dataSource: DataSource,
	private val coverService: CoverService,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		ResourceDatabasePopulator(ClassPathResource("dev-data/books.sql")).apply {
			setSqlScriptEncoding("UTF-8")
		}.execute(dataSource)

		// Covers come from the internet (Open Library) the first time, then from uploads/covers.
		// In the background, so the server is ready right away – covers show up a few seconds later.
		thread(name = "dev-covers", isDaemon = true) {
			coverService.fetchMissingCovers()
		}
	}
}
