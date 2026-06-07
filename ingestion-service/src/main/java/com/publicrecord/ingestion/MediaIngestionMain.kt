package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.GdeltDocConnector
import com.publicrecord.ingestion.connectors.GuardianOpenPlatformConnector
import com.publicrecord.ingestion.connectors.NewsScraperConnector
import com.publicrecord.ingestion.connectors.NewsScraperTarget
import com.publicrecord.ingestion.connectors.BlueskyPostConnector
import com.publicrecord.ingestion.connectors.RSSFeedConnector
import com.publicrecord.ingestion.connectors.XPostConnector
import com.publicrecord.ingestion.connectors.YouTubeDataConnector
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MediaIngestionMain")

/**
 * Runs neutral public-media discovery.
 *
 * This job collects public reporting and public video metadata. It does not
 * decide whether a story is true, false, positive, or controversial. Later
 * normalization and review stages should classify records and attach trust
 * scores, source citations, and politician/bill links.
 */
fun main() {
    val connectors = mutableListOf<ContentConnector>()

    envCsv("GDELT_QUERIES").takeIf { it.isNotEmpty() }?.let { queries ->
        connectors += GdeltDocConnector(
            queries = queries,
            maxRecords = envInt("GDELT_MAX_RECORDS", 25).coerceIn(1, 250)
        )
    }

    envCsv("RSS_FEEDS").forEach { feed ->
        val parts = feed.split("|", limit = 2)
        val feedUrl = parts[0].trim()
        val sourceName = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: hostName(feedUrl) ?: "RSS Feed"
        connectors += RSSFeedConnector(feedUrl, sourceName)
    }

    if (envBool("NEWS_SCRAPER_ENABLED", false)) {
        val targets = envCsv("NEWS_SCRAPER_URLS").map { target ->
            val parts = target.split("|", limit = 2)
            val url = parts[0].trim()
            NewsScraperTarget(
                url = url,
                sourceName = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: hostName(url) ?: "Public News Source"
            )
        }
        if (targets.isNotEmpty()) {
            connectors += NewsScraperConnector(
                targets = targets,
                maxPages = envInt("NEWS_SCRAPER_MAX_PAGES", 10).coerceIn(1, 50)
            )
        } else {
            logger.warn("NEWS_SCRAPER_ENABLED=true but NEWS_SCRAPER_URLS is empty")
        }
    }

    val keys = ApiKeyConfig()
    if (!keys.guardianApiKey.isNullOrBlank()) {
        val queries = envCsv("GUARDIAN_QUERIES")
        if (queries.isNotEmpty()) {
            connectors += GuardianOpenPlatformConnector(
                apiKey = keys.guardianApiKey,
                queries = queries,
                pageSize = envInt("GUARDIAN_PAGE_SIZE", 10).coerceIn(1, 50)
            )
        }
    } else if (envCsv("GUARDIAN_QUERIES").isNotEmpty()) {
        logger.warn("Skipping Guardian ingestion because GUARDIAN_API_KEY is not set")
    }

    if (!keys.youtubeApiKey.isNullOrBlank()) {
        val queries = envCsv("YOUTUBE_QUERIES")
        val channels = envCsv("YOUTUBE_CHANNEL_IDS")
        if (queries.isNotEmpty() || channels.isNotEmpty()) {
            connectors += YouTubeDataConnector(
                apiKey = keys.youtubeApiKey,
                queries = queries,
                channelIds = channels,
                maxResults = envInt("YOUTUBE_MAX_RESULTS", 10).coerceIn(1, 50)
            )
        }
    } else if (envCsv("YOUTUBE_QUERIES").isNotEmpty() || envCsv("YOUTUBE_CHANNEL_IDS").isNotEmpty()) {
        logger.warn("Skipping YouTube ingestion because YOUTUBE_API_KEY is not set")
    }

    val blueskyQueries = envCsv("BLUESKY_QUERIES")
    if (blueskyQueries.isNotEmpty()) {
        connectors += BlueskyPostConnector(
            queries = blueskyQueries,
            limit = envInt("BLUESKY_LIMIT", 25).coerceIn(1, 100)
        )
    }

    val xQueries = envCsv("X_QUERIES")
    if (!keys.xBearerToken.isNullOrBlank()) {
        if (xQueries.isNotEmpty()) {
            connectors += XPostConnector(
                bearerToken = keys.xBearerToken,
                queries = xQueries,
                maxResults = envInt("X_MAX_RESULTS", 10).coerceIn(10, 100)
            )
        }
    } else if (xQueries.isNotEmpty()) {
        logger.warn("Skipping X ingestion because X_BEARER_TOKEN is not set")
    }

    if (connectors.isEmpty()) {
        logger.warn("No media connectors configured. Set GDELT_QUERIES, RSS_FEEDS, GUARDIAN_API_KEY with GUARDIAN_QUERIES, YOUTUBE_API_KEY with YOUTUBE_QUERIES/YOUTUBE_CHANNEL_IDS, BLUESKY_QUERIES, X_BEARER_TOKEN with X_QUERIES, and/or NEWS_SCRAPER_ENABLED=true with NEWS_SCRAPER_URLS.")
        return
    }

    val kafkaBootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS")
    if (kafkaBootstrapServers.isNullOrBlank()) {
        connectors.forEach { connector ->
            val items = connector.fetch()
            logger.info("Fetched {} item(s) from {}. Set KAFKA_BOOTSTRAP_SERVERS to publish them.", items.size, connector.getName())
            items.forEach { logger.info(it.toJson()) }
        }
        return
    }

    val ingestionService = IngestionService(kafkaBootstrapServers)
    connectors.forEach { ingestionService.registerConnector(it) }
    try {
        ingestionService.start()
    } finally {
        ingestionService.shutdown()
    }
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

private fun envCsv(name: String): List<String> {
    return env(name)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
}

private fun envInt(name: String, default: Int): Int = env(name)?.toIntOrNull() ?: default

private fun envBool(name: String, default: Boolean): Boolean {
    return env(name)?.lowercase()?.let { it == "true" || it == "1" || it == "yes" } ?: default
}

private fun hostName(value: String): String? {
    return runCatching { java.net.URI.create(value).host?.removePrefix("www.") }.getOrNull()
}
