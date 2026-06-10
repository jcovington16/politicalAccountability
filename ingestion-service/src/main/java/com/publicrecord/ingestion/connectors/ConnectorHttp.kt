package com.publicrecord.ingestion.connectors

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

interface ConnectorHttpClient {
    fun get(url: String): String
}

interface HeaderAwareConnectorHttpClient : ConnectorHttpClient {
    fun get(url: String, headers: Map<String, String>): String
}

class JavaConnectorHttpClient(
    private val userAgent: String = "public-record-ingestion/0.1",
    private val maxAttempts: Int = (System.getenv("INGEST_HTTP_MAX_ATTEMPTS")?.toIntOrNull() ?: 3).coerceIn(1, 5),
    private val initialBackoffMillis: Long = (System.getenv("INGEST_HTTP_BACKOFF_MS")?.toLongOrNull() ?: 1_000L).coerceAtLeast(100L)
) : HeaderAwareConnectorHttpClient {
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    override fun get(url: String): String {
        return get(url, emptyMap())
    }

    override fun get(url: String, headers: Map<String, String>): String {
        val builder = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .GET()

        headers.forEach { (name, value) -> builder.header(name, value) }
        val request = builder.build()
        var lastStatus = 0
        var lastBody = ""

        repeat(maxAttempts) { attempt ->
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            lastStatus = response.statusCode()
            lastBody = response.body()

            if (response.statusCode() in 200..299) {
                return response.body()
            }

            if (response.statusCode() != 429 && response.statusCode() !in 500..599) {
                error("HTTP request failed with status ${response.statusCode()} for $url")
            }

            if (attempt < maxAttempts - 1) {
                Thread.sleep(initialBackoffMillis * (attempt + 1))
            }
        }

        error("HTTP request failed with status $lastStatus for $url after $maxAttempts attempts. Body: ${lastBody.take(200)}")
    }
}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8)
}
