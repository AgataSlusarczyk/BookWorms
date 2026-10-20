package com.bookworms.server.common

import org.springframework.http.HttpStatus

open class ApiException(
	val status: HttpStatus,
	val code: String,
	override val message: String,
) : RuntimeException(message)
