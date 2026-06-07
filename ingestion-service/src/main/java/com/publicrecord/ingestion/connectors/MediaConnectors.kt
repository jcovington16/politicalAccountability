package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringReader
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * Pulls broad public news coverage from GDELT's free DOC API.
 *
 * GDELT is a discovery source, not a fact authority. We store article URLs and
 * metadata so downstream processing can connect public coverage to politicians,
 * bills, claims, and citations without presenting media coverage as proven fact.
 */
class GdeltDocConnector @JvmOverloads constructor(
    private val queries: List<String>,
    private val maxRecords: Int = 25,
    private val baseUrl: String = "https://api.gdeltproject.org/api/v2/doc/doc",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(GdeltDocConnector::class.java)

    init {
        require(queries.isNotEmpty()) { "At least one GDELT query is required" }
        require(maxRecords in 1..250) { "GDELT maxRecords must be between 1 and 250" }
    }

    override fun getName(): String = "GdeltDocConnector(queries=${queries.size})"

    override fun fetch(): List<RawContentItem> {
        return queries.flatMap { query ->
            val url = "$baseUrl?query=${query.urlEncode()}&mode=ArtList&format=json&maxrecords=$maxRecords&sort=HybridRel"
            logger.info("Fetching GDELT articles for query='{}'", query)
            val root = ContentEventJson.mapper.readTree(httpClient.get(url))
            root.path("articles").mapNotNull { article -> mapArticle(query, article) }
        }.distinctBy { it.sourceUrl }
    }

    private fun mapArticle(query: String, article: JsonNode): RawContentItem? {
        val sourceUrl = article.path("url").asText("").trim()
        val title = article.path("title").asText("").trim()
        if (sourceUrl.isBlank() || title.isBlank()) return null

        return RawContentItem(
            id = stableId("gdelt:$sourceUrl"),
            title = title,
            contentType = "article",
            textBody = article.path("seendate").asText(null)?.let { "GDELT article match for query '$query' first seen at $it." },
            mediaUrl = article.path("socialimage").asText(null)?.takeIf { it.isNotBlank() },
            sourceUrl = sourceUrl,
            publishedDate = normalizeGdeltDate(article.path("seendate").asText(null)),
            source = "GDELT",
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "MEDIA_AGGREGATOR",
                "query" to query,
                "domain" to article.path("domain").asText(null),
                "language" to article.path("language").asText(null),
                "country" to article.path("sourcecountry").asText(null),
                "tone" to article.path("tone").asDoubleOrNull(),
                "sourceCollectionRole" to "DISCOVERY",
                "accountabilityUse" to "Find public reporting that may need citation review before being shown as verified fact"
            ).filterValues { it != null }
        )
    }
}

/**
 * Pulls article records from The Guardian Open Platform.
 *
 * The Guardian is a publisher source, not an official government source. We
 * keep the original article URL and article text/summary as citation material
 * for later review, classification, and politician/bill linking.
 */
class GuardianOpenPlatformConnector @JvmOverloads constructor(
    private val apiKey: String,
    private val queries: List<String>,
    private val pageSize: Int = 10,
    private val baseUrl: String = "https://content.guardianapis.com",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(GuardianOpenPlatformConnector::class.java)

    init {
        require(apiKey.isNotBlank()) { "Guardian API key is required" }
        require(queries.isNotEmpty()) { "At least one Guardian query is required" }
        require(pageSize in 1..50) { "Guardian pageSize must be between 1 and 50" }
    }

    override fun getName(): String = "GuardianOpenPlatformConnector(queries=${queries.size})"

    override fun fetch(): List<RawContentItem> {
        return queries.flatMap { query ->
            val url = "$baseUrl/search?q=${query.urlEncode()}" +
                "&page-size=$pageSize" +
                "&show-fields=trailText,bodyText,thumbnail" +
                "&show-tags=keyword,contributor" +
                "&api-key=${apiKey.urlEncode()}"
            logger.info("Fetching Guardian articles for query='{}'", query)
            val root = ContentEventJson.mapper.readTree(httpClient.get(url))
            root.path("response").path("results").mapNotNull { article -> mapArticle(query, article) }
        }.distinctBy { it.sourceUrl }
    }

    private fun mapArticle(query: String, article: JsonNode): RawContentItem? {
        val title = article.path("webTitle").asText("").trim()
        val sourceUrl = article.path("webUrl").asText("").trim()
        if (title.isBlank() || sourceUrl.isBlank()) return null

        val fields = article.path("fields")
        return RawContentItem(
            id = stableId("guardian:$sourceUrl"),
            title = title,
            contentType = "article",
            textBody = fields.path("bodyText").asText(null) ?: fields.path("trailText").asText(null),
            mediaUrl = fields.path("thumbnail").asText(null)?.takeIf { it.isNotBlank() },
            sourceUrl = sourceUrl,
            publishedDate = normalizeFeedDate(article.path("webPublicationDate").asText(null)),
            source = "The Guardian",
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "PUBLIC_MEDIA",
                "query" to query,
                "guardianId" to article.path("id").asText(null),
                "sectionId" to article.path("sectionId").asText(null),
                "sectionName" to article.path("sectionName").asText(null),
                "pillarName" to article.path("pillarName").asText(null),
                "tags" to article.path("tags").mapNotNull { it.path("webTitle").asText(null) }.takeIf { it.isNotEmpty() },
                "sourceCollectionRole" to "PUBLISHER_API",
                "accountabilityUse" to "Capture Guardian reporting as public media citation material for review"
            ).filterValues { it != null }
        )
    }
}

/**
 * Reads publisher or institution RSS/Atom feeds and turns entries into raw
 * content events. RSS is useful for official press rooms, local outlets, civic
 * watchdogs, and public media sources because it is simple, cheap, and cited.
 */
class RSSFeedConnector @JvmOverloads constructor(
    private val feedUrl: String,
    private val source: String = "RSS Feed",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(RSSFeedConnector::class.java)

    override fun getName(): String = "RSSFeedConnector($source)"

    override fun fetch(): List<RawContentItem> {
        logger.info("Fetching RSS/Atom feed source='{}' url={}", source, feedUrl)
        val document = parseXml(httpClient.get(feedUrl))
        val rssItems = document.getElementsByTagName("item").toElementList()
        val atomEntries = document.getElementsByTagName("entry").toElementList()
        return (rssItems.mapNotNull(::mapRssItem) + atomEntries.mapNotNull(::mapAtomEntry))
            .distinctBy { it.sourceUrl }
    }

    private fun mapRssItem(item: Element): RawContentItem? {
        val title = item.childText("title")?.trim().orEmpty()
        val link = item.childText("link")?.trim().orEmpty()
        if (title.isBlank() || link.isBlank()) return null

        return RawContentItem(
            id = stableId("rss:$source:$link"),
            title = title,
            contentType = "article",
            textBody = item.childText("description") ?: item.childText("content:encoded"),
            mediaUrl = item.firstChildAttribute("media:content", "url") ?: item.firstChildAttribute("enclosure", "url"),
            sourceUrl = link,
            publishedDate = normalizeFeedDate(item.childText("pubDate") ?: item.childText("dc:date")),
            source = source,
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "PUBLIC_MEDIA",
                "feedUrl" to feedUrl,
                "guid" to item.childText("guid"),
                "sourceCollectionRole" to "PUBLISHER_FEED",
                "accountabilityUse" to "Capture public reporting or official feed entries with source citation"
            ).filterValues { it != null }
        )
    }

    private fun mapAtomEntry(entry: Element): RawContentItem? {
        val title = entry.childText("title")?.trim().orEmpty()
        val link = entry.atomLink().orEmpty()
        if (title.isBlank() || link.isBlank()) return null

        return RawContentItem(
            id = stableId("atom:$source:$link"),
            title = title,
            contentType = "article",
            textBody = entry.childText("summary") ?: entry.childText("content"),
            mediaUrl = null,
            sourceUrl = link,
            publishedDate = normalizeFeedDate(entry.childText("published") ?: entry.childText("updated")),
            source = source,
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "PUBLIC_MEDIA",
                "feedUrl" to feedUrl,
                "entryId" to entry.childText("id"),
                "sourceCollectionRole" to "PUBLISHER_FEED",
                "accountabilityUse" to "Capture public reporting or official feed entries with source citation"
            ).filterValues { it != null }
        )
    }
}

/**
 * Uses the official YouTube Data API to discover public videos.
 *
 * YouTube can help accountability work when the video is an official hearing,
 * floor speech, interview, debate, town hall, campaign event, or public
 * statement. This connector stores video metadata and URLs only; transcripts
 * should be fetched later through a reviewed captions/transcription pipeline.
 */
class YouTubeDataConnector @JvmOverloads constructor(
    private val apiKey: String,
    private val queries: List<String> = emptyList(),
    private val channelIds: List<String> = emptyList(),
    private val maxResults: Int = 10,
    private val baseUrl: String = "https://www.googleapis.com/youtube/v3",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(YouTubeDataConnector::class.java)

    init {
        require(apiKey.isNotBlank()) { "YouTube API key is required" }
        require(queries.isNotEmpty() || channelIds.isNotEmpty()) { "At least one YouTube query or channel id is required" }
        require(maxResults in 1..50) { "YouTube maxResults must be between 1 and 50" }
    }

    override fun getName(): String = "YouTubeDataConnector(queries=${queries.size}, channels=${channelIds.size})"

    override fun fetch(): List<RawContentItem> {
        val queryItems = queries.flatMap { query ->
            logger.info("Fetching YouTube videos for query='{}'", query)
            fetchSearch("$baseUrl/search?part=snippet&type=video&order=date&maxResults=$maxResults&q=${query.urlEncode()}&key=${apiKey.urlEncode()}", query, null)
        }
        val channelItems = channelIds.flatMap { channelId ->
            logger.info("Fetching YouTube videos for channelId='{}'", channelId)
            fetchSearch("$baseUrl/search?part=snippet&type=video&order=date&maxResults=$maxResults&channelId=${channelId.urlEncode()}&key=${apiKey.urlEncode()}", null, channelId)
        }
        return (queryItems + channelItems).distinctBy { it.sourceUrl }
    }

    private fun fetchSearch(url: String, query: String?, channelId: String?): List<RawContentItem> {
        val root = ContentEventJson.mapper.readTree(httpClient.get(url))
        return root.path("items").mapNotNull { item -> mapVideo(item, query, channelId) }
    }

    private fun mapVideo(item: JsonNode, query: String?, requestedChannelId: String?): RawContentItem? {
        val videoId = item.path("id").path("videoId").asText("").trim()
        val snippet = item.path("snippet")
        val title = snippet.path("title").asText("").trim()
        if (videoId.isBlank() || title.isBlank()) return null

        val videoUrl = "https://www.youtube.com/watch?v=$videoId"
        return RawContentItem(
            id = stableId("youtube:$videoId"),
            title = title,
            contentType = "video",
            textBody = snippet.path("description").asText(null),
            mediaUrl = videoUrl,
            sourceUrl = videoUrl,
            publishedDate = normalizeFeedDate(snippet.path("publishedAt").asText(null)),
            source = "YouTube",
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "PUBLIC_MEDIA",
                "videoId" to videoId,
                "channelId" to snippet.path("channelId").asText(null),
                "channelTitle" to snippet.path("channelTitle").asText(null),
                "query" to query,
                "requestedChannelId" to requestedChannelId,
                "thumbnailUrl" to (
                    snippet.path("thumbnails").path("high").path("url").asText(null)
                        ?: snippet.path("thumbnails").path("default").path("url").asText(null)
                    ),
                "sourceCollectionRole" to "PUBLIC_VIDEO_DISCOVERY",
                "accountabilityUse" to "Find public statements, hearings, interviews, debates, and event footage for citation review"
            ).filterValues { it != null }
        )
    }
}

private fun parseXml(xml: String): org.w3c.dom.Document {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = false
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    return factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
}

private fun org.w3c.dom.NodeList.toElementList(): List<Element> {
    return (0 until length).mapNotNull { item(it) as? Element }
}

private fun Element.childText(tagName: String): String? {
    return getElementsByTagName(tagName)
        .item(0)
        ?.textContent
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

private fun Element.firstChildAttribute(tagName: String, attributeName: String): String? {
    val node = getElementsByTagName(tagName).item(0) as? Element ?: return null
    return node.getAttribute(attributeName).trim().takeIf { it.isNotBlank() }
}

private fun Element.atomLink(): String? {
    val links = getElementsByTagName("link")
    for (index in 0 until links.length) {
        val link = links.item(index)
        if (link.nodeType != Node.ELEMENT_NODE) continue
        val element = link as Element
        val rel = element.getAttribute("rel").ifBlank { "alternate" }
        val href = element.getAttribute("href").trim()
        if (rel == "alternate" && href.isNotBlank()) return href
    }
    return childText("link")
}

private fun normalizeGdeltDate(value: String?): String {
    if (value.isNullOrBlank()) return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    return runCatching {
        OffsetDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX"))
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }.getOrElse { normalizeFeedDate(value) }
}

private fun normalizeFeedDate(value: String?): String {
    if (value.isNullOrBlank()) return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    val trimmed = value.trim()
    val parsers = listOf(
        { OffsetDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME) },
        { OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME) },
        { OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME) }
    )
    for (parser in parsers) {
        val parsed = runCatching { parser() }.getOrNull()
        if (parsed != null) return parsed.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
    return trimmed
}

private fun JsonNode.asDoubleOrNull(): Double? {
    return if (isMissingNode || isNull || !isNumber) null else asDouble()
}

private fun stableId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray()).toString()
