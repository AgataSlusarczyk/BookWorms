package com.bookworms.server.common

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ApiException::class)
	fun handleApiException(ex: ApiException) =
		error(ex.status, ex.code, ex.message)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
		val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to (it.code ?: "Invalid") }
		return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", fieldErrors)
	}

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleMalformedRequest(ex: HttpMessageNotReadableException) =
		error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed")

	@ExceptionHandler(NoResourceFoundException::class)
	fun handleNotFound(ex: NoResourceFoundException) =
		error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found")

	@ExceptionHandler(HttpRequestMethodNotSupportedException::class)
	fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException) =
		error(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported")

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDenied(ex: AccessDeniedException) =
		error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied")

	@ExceptionHandler(DataIntegrityViolationException::class)
	fun handleDataIntegrity(ex: DataIntegrityViolationException) =
		error(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with existing data")

	@ExceptionHandler(Exception::class)
	fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
		log.error("Unexpected error", ex)
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error")
	}

	private fun error(
		status: HttpStatus,
		code: String,
		message: String,
		fieldErrors: Map<String, String> = emptyMap(),
	) = ResponseEntity.status(status).body(ErrorResponse(code, message, fieldErrors))
}
