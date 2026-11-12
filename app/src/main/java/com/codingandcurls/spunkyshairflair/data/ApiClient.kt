package com.codingandcurls.spunkyshairflair.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class HttpResponse(val code: Int, val body: String)

/** The only place that touches the network, so tests can swap in a fake. */
interface HttpTransport {
    @Throws(IOException::class)
    fun execute(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse
}

class UrlConnectionTransport : HttpTransport {
    override fun execute(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            for ((name, value) in headers) {
                connection.setRequestProperty(name, value)
            }
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code >= 400) connection.errorStream else connection.inputStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            return HttpResponse(code, text)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Talks to the Spunky's Hairflair REST API (see docs/API.md): adds the bearer token,
 * runs requests off the main thread and turns every outcome into an [ApiResult].
 */
class ApiClient(
    baseUrl: String,
    private val transport: HttpTransport,
    private val session: SessionStorage
) {
    private val base = baseUrl.trimEnd('/') + "/"

    suspend fun <T> execute(
        method: String,
        path: String,
        body: JSONObject? = null,
        auth: Boolean = true,
        parse: (String) -> T
    ): ApiResult<T> {
        val headers = mutableMapOf("Accept" to "application/json")
        if (body != null) headers["Content-Type"] = "application/json; charset=utf-8"
        if (auth) {
            session.token()?.let { headers["Authorization"] = "Bearer $it" }
        }

        val response = try {
            withContext(Dispatchers.IO) { transport.execute(method, base + path, headers, body?.toString()) }
        } catch (e: IOException) {
            return ApiResult.Failure(NETWORK_ERROR, 0)
        }

        if (response.code !in 200..299) {
            // A 401 on a logged-in call means the session expired: forget it so the UI sends the user to Login.
            if (response.code == 401 && auth) session.clear()
            return parseError(response)
        }
        return try {
            ApiResult.Success(parse(response.body))
        } catch (e: JSONException) {
            ApiResult.Failure("Unexpected response from the server", response.code)
        }
    }

    private fun parseError(response: HttpResponse): ApiResult.Failure {
        return try {
            val json = JSONObject(response.body)
            val fields = mutableMapOf<String, String>()
            val fieldJson = json.optJSONObject("fields")
            if (fieldJson != null) {
                for (key in fieldJson.keys()) {
                    fields[key] = fieldJson.getString(key)
                }
            }
            ApiResult.Failure(json.optString("error", "Request failed (${response.code})"), response.code, fields)
        } catch (e: JSONException) {
            ApiResult.Failure("Request failed (${response.code})", response.code)
        }
    }

    companion object {
        const val NETWORK_ERROR = "Can't reach the server. Check your connection and try again."
    }
}
