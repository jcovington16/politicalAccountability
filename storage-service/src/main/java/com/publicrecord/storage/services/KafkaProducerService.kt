package com.publicrecord.storage.services

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Kafka service for publishing events
 */
class KafkaProducerService(
    private val bootstrapServers: String,
    private val clientId: String = "political-app-producer"
) {
    private val logger = LoggerFactory.getLogger(KafkaProducerService::class.java)

    private val producer: KafkaProducer<String, String> = initializeProducer()

    private fun initializeProducer(): KafkaProducer<String, String> {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.CLIENT_ID_CONFIG, clientId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.RETRIES_CONFIG, 3)
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
        }
        return KafkaProducer(props)
    }

    /**
     * Publish raw content to Kafka topic
     */
    fun publishRawContent(topic: String, key: String, message: String): Boolean {
        return try {
            val record = ProducerRecord(topic, key, message)
            val metadata = producer.send(record).get()
            logger.info("✅ Published to topic '$topic' (partition: ${metadata.partition()}, offset: ${metadata.offset()})")
            true
        } catch (e: Exception) {
            logger.error("❌ Failed to publish to Kafka: ${e.message}", e)
            false
        }
    }

    /**
     * Publish processed content
     */
    fun publishProcessedContent(messageJson: String): Boolean {
        return publishRawContent("processed-content", UUID.randomUUID().toString(), messageJson)
    }

    /**
     * Test Kafka connectivity
     */
    fun testConnection(): Boolean {
        return try {
            logger.info("Testing Kafka connection to $bootstrapServers...")
            val record = ProducerRecord("test-topic", "test-key", "test-value")
            producer.send(record).get()
            logger.info("✅ Kafka connection successful")
            true
        } catch (e: Exception) {
            logger.error("❌ Kafka connection test failed: ${e.message}", e)
            false
        }
    }

    /**
     * Close the producer
     */
    fun close() {
        try {
            producer.close()
            logger.info("🛑 Kafka producer closed")
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to close Kafka producer: ${e.message}")
        }
    }
}

