package com.publicrecord.ingestion.connectors

import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Placeholder social connector kept intentionally inert.
 *
 * Social platforms change API access frequently and can introduce privacy,
 * scraping, and account-risk concerns. When we add a real connector, it should
 * use official APIs only and preserve the original post URL as a citation.
 */
class TwitterConnector(
    private val apiKey: String,
    private val bearerToken: String
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(TwitterConnector::class.java)

    override fun getName(): String = "TwitterConnector"

    override fun fetch(): List<RawContentItem> {
        logger.info("TwitterConnector is disabled until an official, reviewed integration is implemented")
        return emptyList()
    }
}

/**
 * Small deterministic sample connector for local smoke tests.
 */
class SampleMediaConnector : ContentConnector {
    override fun getName(): String = "SampleMediaConnector"

    override fun fetch(): List<RawContentItem> {
        return listOf(
            RawContentItem(
                id = "sample:media:healthcare-press-conference",
                title = "Press Conference on Healthcare Reform",
                contentType = "video",
                textBody = "A public official discusses healthcare policy priorities.",
                mediaUrl = "https://youtube.com/watch?v=video123",
                sourceUrl = "https://youtube.com/watch?v=video123",
                publishedDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                source = "Sample Media",
                politicianName = null,
                metadata = mapOf(
                    "sourceQuality" to "PUBLIC_MEDIA",
                    "reviewStatus" to "SAMPLE_ONLY"
                )
            )
        )
    }
}
