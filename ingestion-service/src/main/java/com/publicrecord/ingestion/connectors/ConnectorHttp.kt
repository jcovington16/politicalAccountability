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
    private val userAgent: String = "public-record-ingestion/0.1"
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
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            error("HTTP request failed with status ${response.statusCode()} for $url")
        }

        return response.body()
    }
}

fun String.urlEncode(): String {
    return URLEncoder.encode(this, StandardCharsets.UTF_8)
}
