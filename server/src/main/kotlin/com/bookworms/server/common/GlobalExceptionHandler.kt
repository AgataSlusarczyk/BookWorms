package com.bookworms.server.common

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
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

	// A file bigger than spring.servlet.multipart.max-file-size, e.g. a profile photo.
	@ExceptionHandler(MaxUploadSizeExceededException::class)
	fun handleTooBig(ex: MaxUploadSizeExceededException) =
		error(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_BIG", "The file is too big")

	// A form without the expected file, or not a form at all.
	@ExceptionHandler(MissingServletRequestPartException::class, MultipartException::class)
	fun handleBadUpload(ex: Exception) =
		error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The file is missing")

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

	// The phone closed a connection that was still open, e.g. the chat's live stream. There's nobody to answer.
	@ExceptionHandler(AsyncRequestNotUsableException::class)
	fun handleClientGone(ex: AsyncRequestNotUsableException) {
	}

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
	// Always JSON, whatever the request asked for – e.g. the chat's stream asks for text/event-stream,
	// and without this a "not found" there would turn into "406 Not Acceptable".
	) = ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(ErrorResponse(code, message, fieldErrors))
}
