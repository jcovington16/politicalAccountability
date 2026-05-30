package com.publicrecord.processing

import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.common.events.RawContentEvent
import com.publicrecord.common.models.ContentItem
import com.publicrecord.common.models.ProvenanceMetadata
import com.publicrecord.storage.services.KafkaConsumerService
import com.publicrecord.storage.services.KafkaProducerService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * Main processing service that enriches raw content with NLP and entity linking
 */
class ProcessingService(
    private val kafkaBootstrapServers: String,
    private val elasticsearchHost: String
) {
    private val logger = LoggerFactory.getLogger(ProcessingService::class.java)
    
    private val kafkaConsumer = KafkaConsumerService(kafkaBootstrapServers, "processing-service", listOf("raw-content"))
    private val kafkaProducer = KafkaProducerService(kafkaBootstrapServers, "processing-service-producer")
    
    private val enrichers = mutableListOf<ContentEnricher>()

    /**
     * Register a content enricher
     */
    fun registerEnricher(enricher: ContentEnricher) {
        enrichers.add(enricher)
        logger.info("✅ Registered enricher: ${enricher.getName()}")
    }

    /**
     * Start processing messages from Kafka
     */
    fun start() {
        logger.info("🚀 Starting processing service with ${enrichers.size} enricher(s)")

        kafkaConsumer.startConsuming { key, message ->
            try {
                processMessage(key, message)
            } catch (e: Exception) {
                logger.error("❌ Failed to process message: ${e.message}", e)
            }
        }
    }

    /**
     * Process a single raw content message
     */
    private fun processMessage(key: String, messageJson: String) {
        try {
            val event = ContentEventJson.fromJson<RawContentEvent>(messageJson)
            
            // Convert the shared Kafka event into processing-local state.
            val rawContent = RawContentData(
                id = event.id.ifBlank { key },
                title = event.title,
                contentType = event.contentType,
                textBody = event.textBody.orEmpty(),
                mediaUrl = event.mediaUrl.orEmpty(),
                sourceUrl = event.sourceUrl,
                publishedDate = event.publishedDate,
                source = event.source,
                politicianName = event.politicianName
            )

            logger.info("📥 Processing raw content: ${rawContent.title}")

            // Enrich content through pipeline
            var enrichedContent = rawContent
            for (enricher in enrichers) {
                enrichedContent = enricher.enrich(enrichedContent)
            }

            // Convert to ContentItem
            val contentItem = enrichedContent.toContentItem()

            // Publish processed content
            val processedJson = contentItem.toJson()
            val published = kafkaProducer.publishProcessedContent(processedJson)
            
            if (published) {
                logger.info("✅ Published processed content: ${contentItem.title}")
            }
        } catch (e: Exception) {
            logger.error("❌ Error processing message: ${e.message}", e)
        }
    }

    /**
     * Shutdown the service
     */
    fun shutdown() {
        logger.info("🛑 Shutting down processing service")
        kafkaConsumer.stop()
        kafkaProducer.close()
    }
}

/**
 * Interface for content enrichers
 */
interface ContentEnricher {
    fun getName(): String
    fun enrich(rawContent: RawContentData): RawContentData
}

/**
 * Data class for raw content during processing
 */
data class RawContentData(
    val id: String,
    val title: String,
    val contentType: String,
    val textBody: String,
    val mediaUrl: String,
    val sourceUrl: String,
    val publishedDate: String,
    val source: String,
    val politicianName: String?,
    val keywords: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val linkedPoliticianId: UUID? = null,
    val confidence: Float = 1.0f
) {
    fun toContentItem(): ContentItem {
        return ContentItem(
            id = UUID.fromString(id.padEnd(32, '0').substring(0, 32)
                .replaceFirst(Regex("^(.{8})(.{4})(.{4})(.{4})(.{12})$"), "$1-$2-$3-$4-$5")),
            title = title,
            contentType = contentType,
            textBody = textBody.ifEmpty { null },
            mediaUrl = mediaUrl.ifEmpty { null },
            publishedAt = LocalDateTime.parse(publishedDate),
            contentHash = generateHash(sourceUrl + title),
            sourceUrl = sourceUrl,
            politicianId = linkedPoliticianId ?: UUID.randomUUID(),
            keywords = keywords,
            tags = tags,
            provenance = ProvenanceMetadata(
                sourceType = source,
                timestamp = LocalDateTime.now(),
                confidence = confidence
            )
        )
    }

    private companion object {
        fun generateHash(input: String): String {
            return java.security.MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }
    }
}

fun ContentItem.toJson(): String {
    return ContentEventJson.toJson(this)
}
