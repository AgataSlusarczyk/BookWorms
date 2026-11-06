package com.bookworms.server.books

import com.bookworms.server.TestcontainersConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(properties = ["bookworms.covers.locations=classpath:/test-covers/"])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class CoverFilesTest(
	@Autowired private val mockMvc: MockMvc,
) {

	@Test
	fun `cover file is served without logging in`() {
		mockMvc.get("/covers/test-cover.jpg").andExpect {
			status { isOk() }
			content { contentType("image/jpeg") }
			content { string("not-really-a-jpg") }
		}
	}

	@Test
	fun `missing cover returns 404`() {
		mockMvc.get("/covers/nothing-here.jpg").andExpect {
			status { isNotFound() }
		}
	}
}
