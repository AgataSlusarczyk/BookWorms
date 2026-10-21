package com.bookworms.server.common

data class ErrorResponse(
	val code: String,
	val message: String,
	val fieldErrors: Map<String, String> = emptyMap(),
)
