package com.publicrecord.ingestion

import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.common.events.RawContentEvent
import com.publicrecord.storage.services.KafkaProducerService
import org.slf4j.LoggerFactory

typealias RawContentItem = RawContentEvent

/**
 * Main ingestion service that orchestrates data collection from various sources
 */
class IngestionService(
    private val kafkaBootstrapServers: String
) {
    private val logger = LoggerFactory.getLogger(IngestionService::class.java)
    private val kafkaProducer = KafkaProducerService(kafkaBootstrapServers, "ingestion-service")

    private val connectors = mutableListOf<ContentConnector>()

    /**
     * Register a content connector
     */
    fun registerConnector(connector: ContentConnector) {
        connectors.add(connector)
        logger.info("✅ Registered connector: ${connector.getName()}")
    }

    /**
     * Start all connectors and ingest data
     */
    fun start() {
        logger.info("🚀 Starting ingestion service with ${connectors.size} connector(s)")
        
        for (connector in connectors) {
            try {
                logger.info("🔄 Fetching data from: ${connector.getName()}")
                val rawDataItems = connector.fetch()
                logger.info("📥 Fetched ${rawDataItems.size} items from ${connector.getName()}")

                // Publish to Kafka
                for (item in rawDataItems) {
                    val published = kafkaProducer.publishRawContent(
                        "raw-content",
                        item.id,
                        item.toJson()
                    )
                    if (!published) {
                        logger.warn("⚠️ Failed to publish item: ${item.id}")
                    }
                }
            } catch (e: Exception) {
                logger.error("❌ Error fetching from ${connector.getName()}: ${e.message}", e)
            }
        }

        logger.info("✅ Ingestion cycle complete")
    }

    /**
     * Start ingestion in a background thread with periodic execution
     */
    fun startPeriodic(intervalSeconds: Long = 300) {
        Thread {
            logger.info("🔁 Starting periodic ingestion (interval: ${intervalSeconds}s)")
            while (true) {
                try {
                    start()
                    Thread.sleep(intervalSeconds * 1000)
                } catch (e: Exception) {
                    logger.error("❌ Error in periodic ingestion: ${e.message}", e)
                    Thread.sleep(60000) // Wait 1 minute before retrying
                }
            }
        }.apply {
            name = "IngestionThread"
            isDaemon = false
            start()
        }
    }

    /**
     * Shutdown the service
     */
    fun shutdown() {
        logger.info("🛑 Shutting down ingestion service")
        kafkaProducer.close()
    }
}

/**
 * Interface for different content connectors
 */
interface ContentConnector {
    fun getName(): String
    fun fetch(): List<RawContentItem>
}

/**
 * Serialize raw content with the shared Jackson mapper instead of hand-built
 * JSON. This keeps escaping and optional fields correct for Kafka consumers.
 */
fun RawContentItem.toJson(): String = ContentEventJson.toJson(this)
