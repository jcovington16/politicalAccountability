package com.publicrecord.common.models

import java.util.UUID
import java.time.LocalDateTime

/**
 * Represents a content item (article, tweet, video, speech, etc.)
 * This is the core model for the processed content pipeline.
 */
data class ContentItem(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val contentType: String, // "tweet", "article", "speech", "video", etc.
    val textBody: String?,
    val mediaUrl: String?,
    val publishedAt: LocalDateTime,
    val contentHash: String,
    val sourceUrl: String,
    val politicianId: UUID, // Foreign key to politician
    val keywords: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val provenance: ProvenanceMetadata? = null,
    val indexedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Tracks the source and metadata of how content was ingested
 */
data class ProvenanceMetadata(
    val sourceType: String, // "twitter_api", "scraper", "rss", etc.
    val timestamp: LocalDateTime,
    val extractorVersion: String? = null,
    val confidence: Float? = null
)

