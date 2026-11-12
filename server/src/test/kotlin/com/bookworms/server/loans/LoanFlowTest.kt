package com.bookworms.server.loans

import com.bookworms.server.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.util.UUID
import kotlin.test.assertEquals

// Asking, lending and giving back. Agata owns Solaris. Marek and Ola are in her group, Zosia isn't.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class LoanFlowTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val jdbc: JdbcTemplate,
) {

	private lateinit var agata: String
	private lateinit var marek: String
	private lateinit var ola: String
	private lateinit var zosia: String
	private lateinit var solaris: String

	@BeforeEach
	fun prepare() {
		cleanDatabase()
		agata = registerAndLogIn("Agata", "agata@example.com")
		marek = registerAndLogIn("Marek", "marek@example.com")
		ola = registerAndLogIn("Ola", "ola@example.com")
		zosia = registerAndLogIn("Zosia", "zosia@example.com")
		// Groups aren't on the server yet, so the test makes one straight in the database.
		val group = UUID.randomUUID()
		jdbc.update(
			"insert into groups (id, name, invite_code, created_by) values (?, 'Czytelnia', 'ABC123', ?)",
			group, userId("agata@example.com"),
		)
		for (email in listOf("agata@example.com", "marek@example.com", "ola@example.com")) {
			jdbc.update("insert into group_members (user_id, group_id) values (?, ?)", userId(email), group)
		}
		// Added after joining the group, so it's visible there.
		solaris = idOf(
			mockMvc.post("/me/copies") {
				bearer(agata)
				contentType = MediaType.APPLICATION_JSON
				content = """{"isbn": "9788308049461", "title": "Solaris", "authors": "Stanisław Lem"}"""
			}.andExpect { status { isCreated() } },
		)
	}

	// The other test classes share this database, so everything this test made goes away – children first.
	@AfterEach
	fun cleanDatabase() {
		for (table in listOf(
			"notifications", "availability_watchers", "borrowings", "borrow_requests", "copy_group_visibility", "book_copies",
			"group_members", "groups", "books", "users",
		)) {
			jdbc.update("delete from $table")
		}
	}

	@Test
	fun `asking to borrow needs logging in`() {
		mockMvc.post("/copies/$solaris/requests").andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `a group member asks, and both sides see the request`() {
		send(marek).andExpect {
			status { isCreated() }
			jsonPath("$.status") { value("PENDING") }
			jsonPath("$.book.title") { value("Solaris") }
			jsonPath("$.requester.name") { value("Marek") }
			jsonPath("$.owner.name") { value("Agata") }
		}

		mockMvc.get("/me/requests/sent") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(1) }
			// Marek isn't the owner, so it's never his to accept.
			jsonPath("$[0].canAccept") { value(false) }
		}
		mockMvc.get("/me/requests/received") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].requester.name") { value("Marek") }
			jsonPath("$[0].canAccept") { value(true) }
		}
	}

	@Test
	fun `many people can ask for one book, but each person only once at a time`() {
		send(marek).andExpect { status { isCreated() } }
		send(ola).andExpect { status { isCreated() } }

		send(marek).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("REQUEST_ALREADY_SENT") }
		}
		mockMvc.get("/me/requests/received") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(2) } }
	}

	@Test
	fun `the same tap sent twice makes one request`() {
		val first = idOf(send(marek, key = "tap-1").andExpect { status { isCreated() } })
		val second = idOf(send(marek, key = "tap-1").andExpect { status { isCreated() } })

		assertEquals(first, second)
		assertEquals(1, jdbc.queryForObject("select count(*) from borrow_requests", Int::class.java))
	}

	@Test
	fun `only a group member can ask, never for their own book, and only for an available one`() {
		send(zosia).andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("COPY_NOT_FOUND") }
		}
		send(agata).andExpect {
			status { isBadRequest() }
			jsonPath("$.code") { value("OWN_COPY") }
		}

		mockMvc.patch("/copies/$solaris") {
			bearer(agata)
			contentType = MediaType.APPLICATION_JSON
			content = """{"availability": "UNAVAILABLE"}"""
		}
		send(marek).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("COPY_NOT_AVAILABLE") }
		}
	}

	@Test
	fun `after one is accepted the others stay open, but can't be accepted while the book is lent`() {
		val fromMarek = idOf(send(marek))
		val fromOla = idOf(send(ola))

		answer(agata, fromMarek, "accept").andExpect {
			status { isOk() }
			jsonPath("$.status") { value("ACCEPTED") }
		}

		// Ola's request is still there, with Accept grayed out.
		mockMvc.get("/me/requests/received") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].id") { value(fromOla) }
			jsonPath("$[0].canAccept") { value(false) }
		}
		answer(agata, fromOla, "accept").andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("COPY_LENT") }
		}
		// The owner can't remove it while it's lent.
		mockMvc.delete("/copies/$solaris") { bearer(agata) }.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("COPY_LENT") }
		}
		// The owner can still say no to Ola…
		answer(agata, fromOla, "reject").andExpect { jsonPath("$.status") { value("REJECTED") } }
		// …and then Ola can't ask again until the book is back.
		send(ola).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("COPY_LENT") }
		}
	}

	@Test
	fun `both sides see the loan in their tab`() {
		answer(agata, idOf(send(marek)), "accept")

		mockMvc.get("/me/borrowings") { bearer(marek) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].status") { value("BORROWED") }
			jsonPath("$[0].book.title") { value("Solaris") }
			jsonPath("$[0].owner.name") { value("Agata") }
		}
		mockMvc.get("/me/lent") { bearer(agata) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].borrower.name") { value("Marek") }
		}
		// Not hers, not his.
		mockMvc.get("/me/borrowings") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/lent") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `only the owner marks the book as returned, and only once`() {
		answer(agata, idOf(send(marek)), "accept")
		val loan = loanId(agata)

		markReturned(marek, loan).andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("LOAN_NOT_FOUND") }
		}
		markReturned(agata, loan).andExpect {
			status { isOk() }
			jsonPath("$.status") { value("RETURNED") }
		}
		markReturned(agata, loan).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("LOAN_ALREADY_RETURNED") }
		}
		// Gone from both tabs.
		mockMvc.get("/me/borrowings") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/lent") { bearer(agata) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `when the book is back, a waiting request can be accepted and new ones sent`() {
		val fromMarek = idOf(send(marek))
		val fromOla = idOf(send(ola))
		answer(agata, fromMarek, "accept")

		markReturned(agata, loanId(agata)).andExpect { status { isOk() } }

		mockMvc.get("/me/requests/received") { bearer(agata) }.andExpect { jsonPath("$[0].canAccept") { value(true) } }
		// Marek can ask again, now that he gave it back.
		send(marek).andExpect { status { isCreated() } }
		answer(agata, fromOla, "accept").andExpect { status { isOk() } }
	}

	@Test
	fun `removing a book rejects the requests waiting for it`() {
		val fromMarek = idOf(send(marek))

		mockMvc.delete("/copies/$solaris") { bearer(agata) }.andExpect { status { isNoContent() } }

		mockMvc.get("/me/requests/sent") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		answer(agata, fromMarek, "accept").andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("REQUEST_NOT_PENDING") }
		}
	}

	@Test
	fun `only the owner answers, only the one who asked cancels, and an answered request stays answered`() {
		val fromMarek = idOf(send(marek))

		answer(marek, fromMarek, "accept").andExpect {
			status { isNotFound() }
			jsonPath("$.code") { value("REQUEST_NOT_FOUND") }
		}
		answer(agata, fromMarek, "cancel").andExpect { status { isNotFound() } }

		answer(marek, fromMarek, "cancel").andExpect { jsonPath("$.status") { value("CANCELED") } }
		answer(agata, fromMarek, "accept").andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("REQUEST_NOT_PENDING") }
		}
		// Canceled, so Marek can ask again.
		send(marek).andExpect { status { isCreated() } }
	}

	private fun send(token: String, key: String? = null) =
		mockMvc.post("/copies/$solaris/requests") {
			bearer(token)
			if (key != null) {
				contentType = MediaType.APPLICATION_JSON
				content = """{"idempotencyKey": "$key"}"""
			}
		}

	private fun answer(token: String, requestId: String, action: String) =
		mockMvc.post("/requests/$requestId/$action") { bearer(token) }

	@Test
	fun `an available book has no queue – you just ask`() {
		joinQueue(marek).andExpect {
			status { isConflict() }
			jsonPath("$.code") { value("COPY_AVAILABLE") }
		}
		joinQueue(zosia).andExpect { status { isNotFound() } }
		joinQueue(agata).andExpect { jsonPath("$.code") { value("OWN_COPY") } }
	}

	@Test
	fun `while the book is lent, people queue up in order`() {
		answer(agata, idOf(send(marek)), "accept")

		joinQueue(ola).andExpect {
			status { isOk() }
			jsonPath("$.position") { value(1) }
			jsonPath("$.book.title") { value("Solaris") }
		}
		// Joining again just gives the same place back.
		joinQueue(ola).andExpect { jsonPath("$.position") { value(1) } }
		mockMvc.get("/me/queue") { bearer(ola) }.andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].position") { value(1) }
		}

		mockMvc.delete("/copies/$solaris/queue") { bearer(ola) }.andExpect { status { isNoContent() } }
		mockMvc.get("/me/queue") { bearer(ola) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `when the book is back, the queue is cleared`() {
		answer(agata, idOf(send(marek)), "accept")
		joinQueue(ola)

		markReturned(agata, loanId(agata))

		mockMvc.get("/me/queue") { bearer(ola) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `an unavailable book can be queued for until the owner makes it available`() {
		setAvailability("UNAVAILABLE")
		joinQueue(marek).andExpect { jsonPath("$.position") { value(1) } }
		joinQueue(ola).andExpect { jsonPath("$.position") { value(2) } }

		setAvailability("AVAILABLE")

		mockMvc.get("/me/queue") { bearer(marek) }.andExpect { jsonPath("$.length()") { value(0) } }
		mockMvc.get("/me/queue") { bearer(ola) }.andExpect { jsonPath("$.length()") { value(0) } }
	}

	@Test
	fun `the owner hears about a request, and the one who asked hears the answer`() {
		val fromMarek = idOf(send(marek))

		notifications(agata).andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].type") { value("BORROW_REQUESTED") }
			jsonPath("$[0].referenceId") { value(fromMarek) }
			jsonPath("$[0].bookTitle") { value("Solaris") }
			jsonPath("$[0].person.name") { value("Marek") }
			jsonPath("$[0].read") { value(false) }
		}

		answer(agata, fromMarek, "accept")
		notifications(marek).andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].type") { value("REQUEST_ACCEPTED") }
			jsonPath("$[0].person.name") { value("Agata") }
		}
	}

	@Test
	fun `saying no, taking a request back and removing the book are told too`() {
		answer(agata, idOf(send(ola)), "reject")
		notifications(ola).andExpect { jsonPath("$[0].type") { value("REQUEST_REJECTED") } }

		answer(marek, idOf(send(marek)), "cancel")
		// The newest first.
		notifications(agata).andExpect {
			jsonPath("$[0].type") { value("REQUEST_CANCELED") }
			jsonPath("$[0].person.name") { value("Marek") }
		}

		send(marek)
		mockMvc.delete("/copies/$solaris") { bearer(agata) }
		notifications(marek).andExpect { jsonPath("$[0].type") { value("REQUEST_REJECTED") } }
	}

	@Test
	fun `after the return, the borrower and the queue are told`() {
		answer(agata, idOf(send(marek)), "accept")
		joinQueue(ola)

		markReturned(agata, loanId(agata))

		notifications(marek).andExpect { jsonPath("$[0].type") { value("RETURN_CONFIRMED") } }
		notifications(ola).andExpect {
			jsonPath("$.length()") { value(1) }
			jsonPath("$[0].type") { value("COPY_AVAILABLE") }
			jsonPath("$[0].referenceId") { value(solaris) }
			jsonPath("$[0].person.name") { value("Agata") }
		}
	}

	@Test
	fun `mark as read marks only my notifications`() {
		answer(agata, idOf(send(marek)), "reject")

		mockMvc.post("/me/notifications/read") { bearer(agata) }.andExpect { status { isNoContent() } }

		notifications(agata).andExpect { jsonPath("$[0].read") { value(true) } }
		notifications(marek).andExpect { jsonPath("$[0].read") { value(false) } }
	}

	private fun notifications(token: String) =
		mockMvc.get("/me/notifications") { bearer(token) }

	private fun joinQueue(token: String) =
		mockMvc.post("/copies/$solaris/queue") { bearer(token) }

	private fun setAvailability(availability: String) =
		mockMvc.patch("/copies/$solaris") {
			bearer(agata)
			contentType = MediaType.APPLICATION_JSON
			content = """{"availability": "$availability"}"""
		}.andExpect { status { isOk() } }

	private fun markReturned(token: String, loanId: String) =
		mockMvc.post("/borrowings/$loanId/return") { bearer(token) }

	// The owner's only loan, from the Lent tab.
	private fun loanId(owner: String): String =
		idOf(mockMvc.get("/me/lent") { bearer(owner) })

	private fun idOf(result: ResultActionsDsl): String =
		Regex(""""id":"([^"]+)"""").find(result.andReturn().response.contentAsString)!!.groupValues[1]

	private fun userId(email: String): UUID =
		jdbc.queryForObject("select id from users where email = ?", UUID::class.java, email)!!

	private fun registerAndLogIn(name: String, email: String): String {
		mockMvc.post("/auth/register") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name": "$name", "email": "$email", "password": "secret-password"}"""
		}.andExpect { status { isCreated() } }
		val body = mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email": "$email", "password": "secret-password"}"""
		}.andReturn().response.contentAsString
		return Regex(""""token":"([^"]+)"""").find(body)!!.groupValues[1]
	}

	private fun MockHttpServletRequestDsl.bearer(token: String) {
		header("Authorization", "Bearer $token")
	}
}
