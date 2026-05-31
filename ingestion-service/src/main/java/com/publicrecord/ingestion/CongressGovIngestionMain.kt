package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.CongressGovBillConnector
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CongressGovIngestionMain")

fun main() {
    val config = ApiKeyConfig()
    val connector = CongressGovBillConnector(
        apiKey = config.requireCongressApiKey(),
        congress = envInt("CONGRESS_NUMBER", 119),
        billType = env("CONGRESS_BILL_TYPE"),
        limit = envInt("CONGRESS_BILL_LIMIT", 20).coerceIn(1, 250)
    )

    val kafkaBootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS")
    if (kafkaBootstrapServers.isNullOrBlank()) {
        val items = connector.fetch()
        logger.info("Fetched {} Congress.gov bill events. Set KAFKA_BOOTSTRAP_SERVERS to publish them.", items.size)
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
