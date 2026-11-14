package com.bookworms.app.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

// The server explains every error with a code, e.g. {"code": "EMAIL_TAKEN", "message": "..."}.
@Serializable
private data class ErrorDto(val code: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

// The error's code, or null when the answer has none (or isn't the server's JSON at all).
// Useful when one HTTP status can mean different things, e.g. 409 for a taken email or a taken username.
internal fun HttpException.errorCode(): String? =
    try {
        response()?.errorBody()?.string()?.let { errorJson.decodeFromString<ErrorDto>(it).code }
    } catch (e: Exception) {
        null
    }
