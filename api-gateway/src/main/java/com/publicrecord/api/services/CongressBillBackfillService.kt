package com.publicrecord.api.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.publicrecord.common.models.Bill
import com.publicrecord.storage.repositories.BillRepository
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID

/**
 * Cache-aside bill lookup for the early MVP.
 *
 * The API still treats PostgreSQL as the source of truth. When a voter searches
 * for a bill we do not have yet, this service fetches a small official slice
 * from Congress.gov, stores matching bills, and lets the normal repository
 * search return database records.
 */
class CongressBillBackfillService(
    private val billRepository: BillRepository,
    private val apiKey: String? = env("CONGRESS_API_KEY"),
    private val congress: Int = env("CONGRESS_NUMBER")?.toIntOrNull() ?: 119,
    private val baseUrl: String = "https://api.congress.gov/v3",
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val mapper: ObjectMapper = ObjectMapper()
) {
    private val logger = LoggerFactory.getLogger(CongressBillBackfillService::class.java)

    fun backfill(query: String, limit: Int): Int {
        val key = apiKey?.takeIf { it.isNotBlank() } ?: return 0
        val trimmed = query.trim().takeIf { it.length >= 2 } ?: return 0
        val candidates = fetchCandidates(trimmed, key, limit.coerceIn(1, 50))
        var saved = 0

        for (bill in candidates.filter { it.matches(trimmed) }) {
            if (billRepository.save(bill)) saved++
        }

        if (saved > 0) {
            logger.info("Backfilled {} Congress.gov bill(s) for search query", saved)
        }
        return saved
    }

    private fun fetchCandidates(query: String, apiKey: String, limit: Int): List<Bill> {
        val exactBill = parseBillNumber(query)?.let { parsed ->
            fetchExactBill(parsed.type, parsed.number, apiKey)
        }
        if (exactBill != null) return listOf(exactBill)

        val url = "$baseUrl/bill/$congress?format=json&limit=${limit.coerceIn(1, 250)}&api_key=${apiKey.urlEncode()}"
        val root = fetchJson(url) ?: return emptyList()
        val bills = root.path("bills")
        if (!bills.isArray) return emptyList()
        return bills.mapNotNull(::mapBill)
    }

    private fun fetchExactBill(type: String, number: String, apiKey: String): Bill? {
        val url = "$baseUrl/bill/$congress/${type.lowercase()}/${number.urlEncode()}?format=json&api_key=${apiKey.urlEncode()}"
        val root = fetchJson(url) ?: return null
        return mapBill(root.path("bill"))
    }

    private fun fetchJson(url: String): JsonNode? {
        return try {
            val request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                logger.warn("Congress.gov backfill returned status={}", response.statusCode())
                return null
            }
            mapper.readTree(response.body())
        } catch (e: Exception) {
            logger.warn("Congress.gov backfill failed: {}", e.message)
            null
        }
    }

    private fun mapBill(node: JsonNode): Bill? {
        if (node.isMissingNode || node.isNull) return null
        val number = node.text("number") ?: return null
        val type = node.text("type") ?: return null
        val billNumber = "${type.uppercase()}-$number"
        val title = node.text("title") ?: billNumber
        val latestAction = node.path("latestAction")
        val latestActionDate = latestAction.text("actionDate")?.toLocalDateOrNull()
        val introducedDate = node.text("introducedDate")?.toLocalDateOrNull()
            ?: latestActionDate
            ?: LocalDate.now()

        return Bill(
            id = stableUuid("bill:$billNumber"),
            billNumber = billNumber,
            title = title,
            description = latestAction.text("text"),
            introducedBy = null,
            status = "Pending",
            introducedDate = introducedDate,
            lastActionDate = latestActionDate ?: introducedDate,
            billUrl = node.text("url") ?: "https://www.congress.gov/bill/$congress-th-congress/${type.lowercase()}-bill/$number"
        )
    }

    private fun Bill.matches(query: String): Boolean {
        val value = query.lowercase()
        return "$billNumber $title ${description.orEmpty()} ${billUrl.orEmpty()}".lowercase().contains(value) ||
            parseBillNumber(query)?.let { "${it.type}-${it.number}".equals(billNumber, ignoreCase = true) } == true
    }

    private fun parseBillNumber(query: String): ParsedBill? {
        val match = Regex("""\b(HR|H\.R\.|S|HJRES|SJRES|HCONRES|SCONRES|HRES|SRES)[\s.-]*(\d+)\b""", RegexOption.IGNORE_CASE)
            .find(query)
            ?: return null
        val type = match.groupValues[1].replace(".", "").uppercase()
        return ParsedBill(type, match.groupValues[2])
    }

    private fun stableUuid(input: String): UUID = UUID.nameUUIDFromBytes(input.toByteArray(StandardCharsets.UTF_8))

    private data class ParsedBill(val type: String, val number: String)
}

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return try {
        LocalDate.parse(take(10))
    } catch (_: Exception) {
        null
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
