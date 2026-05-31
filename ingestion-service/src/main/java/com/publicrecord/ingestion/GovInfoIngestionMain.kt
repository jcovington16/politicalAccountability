package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.GovInfoPackageConnector
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

private val logger = LoggerFactory.getLogger("GovInfoIngestionMain")

fun main() {
    val config = ApiKeyConfig()
    val connector = GovInfoPackageConnector(
        apiKey = config.requireGovInfoApiKey(),
        collection = env("GOVINFO_COLLECTION") ?: "BILLS",
        startDateTime = env("GOVINFO_START_DATE_TIME") ?: OffsetDateTime.now().minusDays(7).toString(),
        pageSize = envInt("GOVINFO_PAGE_SIZE", 10).coerceIn(1, 100)
    )

    val kafkaBootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS")
    if (kafkaBootstrapServers.isNullOrBlank()) {
        val items = connector.fetch()
        logger.info("Fetched {} GovInfo package events. Set KAFKA_BOOTSTRAP_SERVERS to publish them.", items.size)
        items.forEach { logger.info(it.toJson()) }
        return
    }

    val ingestionService = IngestionService(kafkaBootstrapServers)
    ingestionService.registerConnector(connector)
    try {
        ingestionService.start()
    } finally {
        ingestionService.shutdown()
    }
}

private fun env(name: String): String? {
    return System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
}

private fun envInt(name: String, default: Int): Int {
    return env(name)?.toIntOrNull() ?: default
}
