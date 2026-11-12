package com.codingandcurls.spunkyshairflair.data

/** Outcome of an API call. [Failure.code] is the HTTP status, or 0 when the server could not be reached. */
sealed class ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>()

    data class Failure(
        val message: String,
        val code: Int = 0,
        /** Per-field messages from the server, e.g. {"email": "An account with this email already exists"}. */
        val fields: Map<String, String> = emptyMap()
    ) : ApiResult<Nothing>()
}
