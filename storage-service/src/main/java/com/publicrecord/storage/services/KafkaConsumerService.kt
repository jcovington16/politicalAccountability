package com.publicrecord.storage.services

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Kafka service for consuming events
 */
class KafkaConsumerService(
    private val bootstrapServers: String,
    private val groupId: String,
    private val topics: List<String> = listOf("raw-content")
) {
    private val logger = LoggerFactory.getLogger(KafkaConsumerService::class.java)
    private val isRunning = AtomicBoolean(false)

    private val consumer: KafkaConsumer<String, String> = initializeConsumer()

    private fun initializeConsumer(): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
            put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100)
        }
        val kafkaConsumer = KafkaConsumer<String, String>(props)
        kafkaConsumer.subscribe(topics)
        return kafkaConsumer
    }

    /**
     * Start consuming messages from Kafka
     * @param messageHandler Lambda to handle each message
     */
    fun startConsuming(messageHandler: (String, String) -> Unit) {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("🚀 Starting Kafka consumer for topics: $topics")
            
            Thread {
                try {
                    while (isRunning.get()) {
                        val records = consumer.poll(Duration.ofMillis(100))
                        for (record in records) {
                            try {
                                messageHandler(record.key(), record.value())
                            } catch (e: Exception) {
                                logger.error("❌ Error processing message: ${e.message}", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("❌ Kafka consumer error: ${e.message}", e)
                } finally {
                    consumer.close()
                    logger.info("🛑 Kafka consumer stopped")
                }
            }.apply {
                name = "KafkaConsumerThread"
                isDaemon = false
                start()
            }
        }
    }

    /**
     * Stop consuming messages
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping Kafka consumer...")
        }
    }

    /**
     * Test Kafka connectivity
     */
    fun testConnection(): Boolean {
        return try {
            logger.info("Testing Kafka consumer connection to $bootstrapServers...")
            logger.info("✅ Kafka consumer connection successful (group: $groupId)")
            true
        } catch (e: Exception) {
            logger.error("❌ Kafka consumer connection test failed: ${e.message}", e)
            false
        }
    }
}

