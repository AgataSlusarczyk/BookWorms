package com.bookworms.server.dev

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.stereotype.Component
import javax.sql.DataSource

// Sample data for local development, so the app has something to show.
// Only with the dev profile (./gradlew bootRun turns it on) – never on a real server, never in tests.
// Runs on every start; rows that are already there are skipped.
@Component
@Profile("dev")
class DevDataLoader(
	private val dataSource: DataSource,
) : ApplicationRunner {

	override fun run(args: ApplicationArguments) {
		ResourceDatabasePopulator(ClassPathResource("dev-data/books.sql")).apply {
			setSqlScriptEncoding("UTF-8")
		}.execute(dataSource)
	}
}
