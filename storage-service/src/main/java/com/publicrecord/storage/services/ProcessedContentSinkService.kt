package com.publicrecord.storage.services

import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.common.models.ContentItem
import com.publicrecord.storage.repositories.ContentItemRepository
import org.slf4j.LoggerFactory

/**
 * Consumes processed content events and persists them to the current source of
 * truth: PostgreSQL through ContentItemRepository.
 *
 * Elasticsearch and MinIO should be added here once their content indexing and
 * media-storage contracts are concrete. Keeping this sink small makes the
 * processing pipeline useful now without inventing incomplete side effects.
 */
class ProcessedContentSinkService(
    private val kafkaBootstrapServers: String,
    private val contentItemRepository: ContentItemRepository
) {
    private val logger = LoggerFactory.getLogger(ProcessedContentSinkService::class.java)
    private val consumer = KafkaConsumerService(
        bootstrapServers = kafkaBootstrapServers,
        groupId = "processed-content-sink",
        topics = listOf("processed-content")
    )

    fun start() {
        logger.info("Starting processed content sink")
        consumer.startConsuming { key, message ->
            persistProcessedContent(key, message)
        }
    }

    fun stop() {
        logger.info("Stopping processed content sink")
        consumer.stop()
    }

    private fun persistProcessedContent(key: String, messageJson: String) {
        try {
            val contentItem = ContentEventJson.fromJson<ContentItem>(messageJson)
            if (contentItemRepository.save(contentItem)) {
                logger.info("Saved processed content item id={} kafkaKey={}", contentItem.id, key)
            } else {
                logger.warn("Repository rejected processed content item id={} kafkaKey={}", contentItem.id, key)
            }
        } catch (e: Exception) {
            logger.error("Failed to persist processed content kafkaKey={}: {}", key, e.message, e)
        }
    }
}
