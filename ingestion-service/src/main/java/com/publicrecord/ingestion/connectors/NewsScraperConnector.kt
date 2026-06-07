package com.publicrecord.ingestion.connectors

import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class NewsScraperTarget(
    val url: String,
    val sourceName: String
)

interface NewsPageFetcher {
    fun fetch(url: String): String
}

class JsoupNewsPageFetcher(
    private val userAgent: String = DEFAULT_SCRAPER_USER_AGENT
) : NewsPageFetcher {
    override fun fetch(url: String): String {
        return Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(10_000)
            .followRedirects(true)
            .get()
            .outerHtml()
    }
}

interface RobotsPolicy {
    fun isAllowed(url: String): Boolean
}

class BasicRobotsPolicy(
    private val userAgent: String = DEFAULT_SCRAPER_USER_AGENT
) : RobotsPolicy {
    private val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    private val cache = mutableMapOf<String, List<RobotsRule>>()

    override fun isAllowed(url: String): Boolean {
        val uri = URI.create(url)
        val host = uri.host ?: return false
        val rules = cache.getOrPut("${uri.scheme}://$host") { fetchRules(uri) }
        if (rules.isEmpty()) return true

        val path = uri.rawPath.ifBlank { "/" }
        val applicable = rules.filter { it.userAgent == "*" || userAgent.contains(it.userAgent, ignoreCase = true) }
        val match = applicable
            .filter { path.startsWith(it.path) }
            .maxByOrNull { it.path.length }
        return match?.allow ?: true
    }

    private fun fetchRules(uri: URI): List<RobotsRule> {
        return runCatching {
            val robotsUri = URI("${uri.scheme}://${uri.host}/robots.txt")
            val request = HttpRequest.newBuilder(robotsUri)
                .header("User-Agent", userAgent)
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) emptyList() else parseRobots(response.body())
        }.getOrDefault(emptyList())
    }

    private fun parseRobots(body: String): List<RobotsRule> {
        val rules = mutableListOf<RobotsRule>()
        var currentAgents = mutableListOf<String>()

        body.lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val key = line.substringBefore(":").trim().lowercase()
                val value = line.substringAfter(":", "").trim()
                when (key) {
                    "user-agent" -> currentAgents = mutableListOf(value)
                    "disallow" -> if (value.isNotBlank()) currentAgents.forEach { rules += RobotsRule(it, value, allow = false) }
                    "allow" -> if (value.isNotBlank()) currentAgents.forEach { rules += RobotsRule(it, value, allow = true) }
                }
            }

        return rules
    }
}

data class RobotsRule(
    val userAgent: String,
    val path: String,
    val allow: Boolean
)

/**
 * A narrow, allowlisted HTML article scraper for legitimate public sources.
 *
 * This is intentionally not a crawler. It only fetches explicit URLs configured
 * by an operator, requires HTTPS by default, checks a basic robots.txt policy,
 * and emits raw article events as citation candidates. The app must not treat
 * scraped article text as verified fact without later classification/review.
 */
class NewsScraperConnector @JvmOverloads constructor(
    private val targets: List<NewsScraperTarget>,
    private val maxPages: Int = 25,
    private val requireHttps: Boolean = true,
    private val allowedHosts: Set<String> = targets.mapNotNull { URI.create(it.url).host?.removePrefix("www.") }.toSet(),
    private val pageFetcher: NewsPageFetcher = JsoupNewsPageFetcher(),
    private val robotsPolicy: RobotsPolicy = BasicRobotsPolicy()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(NewsScraperConnector::class.java)

    init {
        require(targets.isNotEmpty()) { "At least one scraper target is required" }
        require(maxPages in 1..50) { "News scraper maxPages must be between 1 and 50" }
        require(allowedHosts.isNotEmpty()) { "At least one allowed host is required" }
    }

    override fun getName(): String = "NewsScraperConnector(targets=${targets.size})"

    override fun fetch(): List<RawContentItem> {
        return targets.take(maxPages).mapNotNull { target ->
            val uri = URI.create(target.url)
            val host = uri.host?.removePrefix("www.").orEmpty()

            if (requireHttps && uri.scheme != "https") {
                logger.warn("Skipping non-HTTPS scraper target: {}", target.url)
                return@mapNotNull null
            }
            if (host !in allowedHosts) {
                logger.warn("Skipping scraper target outside allowlist host={} url={}", host, target.url)
                return@mapNotNull null
            }
            if (!robotsPolicy.isAllowed(target.url)) {
                logger.warn("Skipping scraper target blocked by robots.txt: {}", target.url)
                return@mapNotNull null
            }

            runCatching {
                mapDocument(target, Jsoup.parse(pageFetcher.fetch(target.url), target.url))
            }.getOrElse { error ->
                logger.warn("Failed to scrape target url={} reason={}", target.url, error.message)
                null
            }
        }.distinctBy { it.sourceUrl }
    }

    private fun mapDocument(target: NewsScraperTarget, document: Document): RawContentItem? {
        val canonicalUrl = document.selectFirst("link[rel=canonical]")?.attr("abs:href")?.takeIf { it.isNotBlank() } ?: target.url
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: document.title().takeIf { it.isNotBlank() }
            ?: return null
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("meta[name=description]")?.attr("content")?.takeIf { it.isNotBlank() }
        val published = document.selectFirst("meta[property=article:published_time]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("time[datetime]")?.attr("datetime")?.takeIf { it.isNotBlank() }
        val image = document.selectFirst("meta[property=og:image]")?.attr("content")?.takeIf { it.isNotBlank() }

        return RawContentItem(
            id = stableScraperId(canonicalUrl),
            title = title.trim(),
            contentType = "article",
            textBody = description,
            mediaUrl = image,
            sourceUrl = canonicalUrl,
            publishedDate = normalizeScrapedDate(published),
            source = target.sourceName,
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "PUBLIC_MEDIA",
                "sourceCollectionRole" to "ALLOWLISTED_PUBLIC_PAGE",
                "scraperPolicy" to "https-only, allowlisted-host, robots-checked, no-login, no-paywall-bypass",
                "accountabilityUse" to "Capture public article metadata as citation material for later review",
                "originalUrl" to target.url,
                "host" to URI.create(canonicalUrl).host?.removePrefix("www.")
            ).filterValues { it != null }
        )
    }
}

private fun normalizeScrapedDate(value: String?): String {
    if (value.isNullOrBlank()) return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    return runCatching {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }.getOrDefault(value)
}

private fun stableScraperId(value: String): String = UUID.nameUUIDFromBytes("scraper:$value".toByteArray()).toString()

private const val DEFAULT_SCRAPER_USER_AGENT = "PublicRecordBot/0.1 (+https://public-record.local; citation discovery)"
