package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Ingests federal bill metadata from Congress.gov.
 *
 * Congress.gov is the first connector because it is an official source for
 * federal bills, sponsors, actions, summaries, and bill text links. This
 * connector emits raw bill events; downstream processing/storage should map
 * them into normalized bill, citation, action, and sponsor tables.
 */
class CongressGovBillConnector(
    private val apiKey: String,
    private val congress: Int,
    private val billType: String? = null,
    private val limit: Int = 20,
    private val baseUrl: String = "https://api.congress.gov/v3",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(CongressGovBillConnector::class.java)

    init {
        require(apiKey.isNotBlank()) { "Congress.gov API key is required" }
        require(congress > 0) { "Congress number must be positive" }
        require(limit in 1..250) { "Congress.gov bill limit must be between 1 and 250" }
    }

    override fun getName(): String = "CongressGovBillConnector(congress=$congress)"

    override fun fetch(): List<RawContentItem> {
        val endpoint = buildBillListUrl()
        logger.info("Fetching Congress.gov bills for congress={} billType={} limit={}", congress, billType ?: "all", limit)

        val body = httpClient.get(endpoint)
        val root = ContentEventJson.mapper.readTree(body)
        val bills = root.path("bills")

        if (!bills.isArray) {
            logger.warn("Congress.gov response did not contain a bills array")
            return emptyList()
        }

        return bills.mapNotNull(::mapBill)
    }

    private fun buildBillListUrl(): String {
        val path = if (billType.isNullOrBlank()) {
            "$baseUrl/bill/$congress"
        } else {
            "$baseUrl/bill/$congress/${billType.lowercase()}"
        }

        return "$path?format=json&limit=${limit.coerceIn(1, 250)}&api_key=${apiKey.urlEncode()}"
    }

    private fun mapBill(node: JsonNode): RawContentItem? {
        val number = node.text("number") ?: return null
        val type = node.text("type") ?: billType ?: "bill"
        val title = node.text("title") ?: "$type $number"
        val congressNumber = node.int("congress") ?: congress
        val latestAction = node.path("latestAction")
        val latestActionDate = latestAction.text("actionDate")
        val latestActionText = latestAction.text("text")
        val updateDate = node.text("updateDate") ?: LocalDate.now().toString()
        val sourceUrl = node.text("url") ?: "$baseUrl/bill/$congressNumber/${type.lowercase()}/$number"
        val billNumber = "${type.uppercase()}-$number"

        return RawContentItem(
            id = "congress.gov:bill:$congressNumber:${type.lowercase()}:$number",
            title = "$billNumber: $title",
            contentType = "bill",
            textBody = latestActionText ?: title,
            mediaUrl = null,
            sourceUrl = sourceUrl,
            publishedDate = latestActionDate ?: updateDate,
            source = "Congress.gov",
            politicianName = null,
            metadata = mapOf(
                "sourceSystem" to "congress.gov",
                "sourceQuality" to "OFFICIAL_RECORD",
                "congress" to congressNumber,
                "billType" to type.uppercase(),
                "billNumber" to billNumber,
                "originChamber" to node.text("originChamber"),
                "latestActionDate" to latestActionDate,
                "latestActionText" to latestActionText,
                "updateDate" to updateDate
            )
        )
    }
}

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}

private fun JsonNode.int(field: String): Int? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asInt()
}
