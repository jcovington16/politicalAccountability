package com.publicrecord.api

import com.publicrecord.common.models.Politician
import com.publicrecord.common.models.ContentItem
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * HTTP client for communicating with storage service
 *
 * Sidelined for the current early-stage architecture.
 *
 * The long-term goal is still to split storage-service into its own HTTP
 * microservice. Right now storage-service is only a Gradle module with
 * repositories and infrastructure clients, so the API gateway uses those
 * classes in-process instead of calling a non-existent service on port 8082.
 *
 * Keep this class as a marker for the future extraction, but do not wire new
 * API resources through it until storage-service has a real server, routes,
 * health checks, and deployment entry in docker-compose.
 */
class StorageServiceClient {
    private val logger = LoggerFactory.getLogger(StorageServiceClient::class.java)
    private val baseUrl = "http://storage-service:8082"
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()

    // ========== Politician Methods ==========

    fun fetchPoliticianById(id: String): Politician? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/politicians/$id")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else null
                } else {
                    logger.warn("Failed to fetch politician: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching politician: ${e.message}", e)
            null
        }
    }

    fun searchPoliticiansByName(nameQuery: String): List<Politician> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/politicians/search?name=$nameQuery")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error searching politicians: ${e.message}", e)
            emptyList()
        }
    }

    fun getPoliticiansByState(state: String): List<Politician> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/politicians/state/$state")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching politicians by state: ${e.message}", e)
            emptyList()
        }
    }

    fun getPoliticiansByParty(party: String): List<Politician> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/politicians/party/$party")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching politicians by party: ${e.message}", e)
            emptyList()
        }
    }

    // ========== Content Methods ==========

    fun fetchContentByPolitician(politicianId: String, limit: Int = 50, offset: Int = 0): List<ContentItem> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/content/politician/$politicianId?limit=$limit&offset=$offset")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching content for politician: ${e.message}", e)
            emptyList()
        }
    }

    fun fetchContentByType(politicianId: String, contentType: String, limit: Int = 50): List<ContentItem> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/content/politician/$politicianId/type/$contentType?limit=$limit")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching content by type: ${e.message}", e)
            emptyList()
        }
    }

    fun searchContentByKeyword(politicianId: String, keyword: String, limit: Int = 50): List<ContentItem> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/content/politician/$politicianId/search?keyword=$keyword&limit=$limit")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error searching content: ${e.message}", e)
            emptyList()
        }
    }

    fun fetchContentByDateRange(
        politicianId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        limit: Int = 100
    ): List<ContentItem> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/content/politician/$politicianId/daterange?startDate=$startDate&endDate=$endDate&limit=$limit")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyList()
                } else {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching content by date range: ${e.message}", e)
            emptyList()
        }
    }

    fun getTimelineStatistics(politicianId: String): Map<String, Any> {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/content/politician/$politicianId/stats")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) mapper.readValue(body) else emptyMap()
                } else {
                    emptyMap()
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching timeline statistics: ${e.message}", e)
            emptyMap()
        }
    }
}
