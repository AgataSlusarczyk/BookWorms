package com.bookworms.server.dev

import com.bookworms.server.books.CoverService
import com.bookworms.server.users.GlobalRole
import com.bookworms.server.users.User
import com.bookworms.server.users.UserRepository
import com.bookworms.server.users.UserStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
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
	private val userRepository: UserRepository,
	private val passwordEncoder: PasswordEncoder,
	private val transactions: TransactionTemplate,
) : ApplicationRunner {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun run(args: ApplicationArguments) {
		ResourceDatabasePopulator(ClassPathResource("dev-data/books.sql")).apply {
			setSqlScriptEncoding("UTF-8")
		}.execute(dataSource)

		transactions.executeWithoutResult { addAdmin() }

		// Covers come from the internet (Open Library) the first time, then from uploads/covers.
		// In the background, so the server is ready right away – covers show up a few seconds later.
		thread(name = "dev-covers", isDaemon = true) {
			coverService.fetchMissingCovers()
		}
	}

	// The system admin account, to log into the admin panel on the phone: admin@admin.com / admin.
	// It's the account with the username "admin". On every start it gets this email and password back, and is made
	// admin and unblocked again – so whatever was changed while testing, these always work.
	// The password is shorter than sign-up allows (8), which is fine: logging in doesn't check the length.
	// Not in a migration on purpose: a real server mustn't have an admin whose password is written here.
	// There, make someone admin by hand: UPDATE users SET global_role = 'SYSTEM_ADMIN' WHERE email = '…';
	private fun addAdmin() {
		val admin = userRepository.findByUsername(ADMIN_USERNAME) ?: userRepository.findByEmail(ADMIN_EMAIL)
		if (admin != null) {
			val emailTaken = admin.email != ADMIN_EMAIL && userRepository.existsByEmail(ADMIN_EMAIL)
			if (emailTaken) {
				log.warn("Can't give the dev admin the email '{}': another account has it", ADMIN_EMAIL)
			} else {
				admin.email = ADMIN_EMAIL
			}
			admin.passwordHash = passwordEncoder.encode(ADMIN_PASSWORD)!!
			admin.globalRole = GlobalRole.SYSTEM_ADMIN
			admin.status = UserStatus.ACTIVE
			log.info("Dev admin account: {} / {}", admin.email, ADMIN_PASSWORD)
			return
		}
		userRepository.save(
			User(
				name = "Admin",
				email = ADMIN_EMAIL,
				passwordHash = passwordEncoder.encode(ADMIN_PASSWORD)!!,
				username = ADMIN_USERNAME,
			).apply { globalRole = GlobalRole.SYSTEM_ADMIN },
		)
		log.info("Added the dev admin account: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD)
	}

	private companion object {
		const val ADMIN_EMAIL = "admin@admin.com"
		const val ADMIN_USERNAME = "admin"
		const val ADMIN_PASSWORD = "admin"
	}
}
