package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import java.time.LocalDate

/**
 * Ingests official GovInfo package summaries.
 *
 * GovInfo is the best companion to Congress.gov for bill text, Congressional
 * Record references, public laws, and other official GPO documents. This first
 * connector reads package summaries from a collection update feed and emits
 * raw official-document events. Downstream processors can attach these events
 * as bill text/source citations or Congressional Record/public-law documents.
 */
class GovInfoPackageConnector(
    private val apiKey: String,
    private val collection: String = "BILLS",
    private val startDateTime: String,
    private val pageSize: Int = 10,
    private val baseUrl: String = "https://api.govinfo.gov",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(GovInfoPackageConnector::class.java)

    init {
        require(apiKey.isNotBlank()) { "GovInfo API key is required" }
        require(collection.isNotBlank()) { "GovInfo collection is required" }
        require(pageSize in 1..100) { "GovInfo page size must be between 1 and 100" }
        require(startDateTime.isNotBlank()) { "GovInfo startDateTime is required" }
    }

    override fun getName(): String = "GovInfoPackageConnector(collection=${collection.uppercase()})"

    override fun fetch(): List<RawContentItem> {
        val packages = fetchPackageList()
        logger.info("Fetched {} GovInfo packages from collection={}", packages.size, collection.uppercase())
        return packages.mapNotNull { packageNode ->
            val packageId = packageNode.text("packageId") ?: return@mapNotNull null
            mapPackageSummary(packageId, packageNode.text("lastModified"))
        }
    }

    private fun fetchPackageList(): List<JsonNode> {
        val url = "$baseUrl/collections/${collection.uppercase()}/${startDateTime.urlEncode()}" +
            "?pageSize=${pageSize.coerceIn(1, 100)}&offsetMark=*&api_key=${apiKey.urlEncode()}"
        val root = ContentEventJson.mapper.readTree(httpClient.get(url))
        val packages = root.path("packages")
        if (!packages.isArray) return emptyList()
        return packages.toList()
    }

    private fun mapPackageSummary(packageId: String, fallbackModified: String?): RawContentItem? {
        val url = "$baseUrl/packages/${packageId.urlEncode()}/summary?api_key=${apiKey.urlEncode()}"
        val summary = ContentEventJson.mapper.readTree(httpClient.get(url))
        val title = summary.text("title") ?: packageId
        val collectionCode = summary.text("collectionCode") ?: collection.uppercase()
        val collectionName = summary.text("collectionName") ?: collectionCode
        val dateIssued = summary.text("dateIssued")
        val lastModified = summary.text("lastModified") ?: fallbackModified ?: LocalDate.now().toString()
        val detailsLink = summary.text("detailsLink") ?: "https://www.govinfo.gov/app/details/$packageId"
        val download = summary.path("download")

        return RawContentItem(
            id = "govinfo:package:$packageId",
            title = title,
            contentType = "official_document",
            textBody = title,
            mediaUrl = download.text("pdfLink") ?: download.text("txtLink"),
            sourceUrl = detailsLink,
            publishedDate = dateIssued ?: lastModified,
            source = "GovInfo",
            politicianName = null,
            metadata = mapOf(
                "sourceSystem" to "govinfo",
                "sourceQuality" to "OFFICIAL_RECORD",
                "packageId" to packageId,
                "collectionCode" to collectionCode,
                "collectionName" to collectionName,
                "category" to summary.text("category"),
                "branch" to summary.text("branch"),
                "documentType" to summary.text("documentType"),
                "congress" to summary.text("congress"),
                "session" to summary.text("session"),
                "dateIssued" to dateIssued,
                "lastModified" to lastModified,
                "pdfLink" to download.text("pdfLink"),
                "txtLink" to download.text("txtLink"),
                "xmlLink" to download.text("xmlLink"),
                "modsLink" to download.text("modsLink"),
                "zipLink" to download.text("zipLink")
            )
        )
    }
}

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}
