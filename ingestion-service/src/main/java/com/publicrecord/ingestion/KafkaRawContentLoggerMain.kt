package com.publicrecord.ingestion

import com.publicrecord.storage.services.KafkaConsumerService
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

private val logger = LoggerFactory.getLogger("KafkaRawContentLogger")

fun main() {
    val bootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092"
    val topic = env("KAFKA_RAW_TOPIC") ?: "raw-content"
    val groupId = env("KAFKA_RAW_LOGGER_GROUP") ?: "raw-content-logger"
    val consumer = KafkaConsumerService(bootstrapServers, groupId, listOf(topic))
    val latch = CountDownLatch(1)

    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Stopping raw content logger")
        consumer.stop()
        latch.countDown()
    })

    logger.info("Starting raw content logger bootstrapServers={} topic={} groupId={}", bootstrapServers, topic, groupId)
    consumer.startConsuming { key, message ->
        logger.info("raw-content key={} message={}", key, message)
    }

    latch.await()
}

private fun env(name: String): String? {
    return System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
}
