package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.CongressGovBillConnector
import com.publicrecord.ingestion.connectors.GovInfoPackageConnector
import com.publicrecord.ingestion.normalization.OfficialDataNormalizer
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import java.time.LocalDate

/**
 * Fetches trusted official sources and writes normalized rows into Postgres.
 *
 * This is intentionally separate from the Kafka raw connector commands:
 * - raw connector commands prove we can fetch and publish events;
 * - this command is the local MVP path for turning those events into bills,
 *   bill actions, source citations, and auditable import results.
 */
fun main() {
    val logger = LoggerFactory.getLogger("OfficialDataNormalizeMain")
    val apiKeys = ApiKeyConfig()
    val events = mutableListOf<RawContentItem>()

    apiKeys.congressApiKey?.let { apiKey ->
        val congress = envInt("CONGRESS_NUMBER", 119)
        val limit = envInt("CONGRESS_BILL_LIMIT", 20).coerceIn(1, 250)
        val billType = env("CONGRESS_BILL_TYPE")
        events += CongressGovBillConnector(
            apiKey = apiKey,
            congress = congress,
            billType = billType,
            limit = limit
        ).fetch()
    } ?: logger.warn("Skipping Congress.gov normalization because CONGRESS_API_KEY is not set")

    apiKeys.govInfoApiKey?.let { apiKey ->
        val pageSize = envInt("GOVINFO_PAGE_SIZE", 10).coerceIn(1, 100)
        val startDateTime = env("GOVINFO_START_DATE_TIME") ?: defaultGovInfoStartDateTime()
        events += GovInfoPackageConnector(
            apiKey = apiKey,
            collection = env("GOVINFO_COLLECTION") ?: "BILLS",
            startDateTime = startDateTime,
            pageSize = pageSize
        ).fetch()
    } ?: logger.warn("Skipping GovInfo normalization because GOVINFO_API_KEY is not set")

    require(events.isNotEmpty()) {
        "No official source events were fetched. Set at least one official API key in .env."
    }

    val databaseUrl = env("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/political_data"
    val databaseUser = env("DATABASE_USER") ?: env("DB_USERNAME") ?: "postgres"
    val databasePassword = env("DATABASE_PASSWORD") ?: env("DB_PASSWORD") ?: "postgres"

    DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).use { conn ->
        conn.autoCommit = false
        try {
            val result = OfficialDataNormalizer(conn).normalize(
                sourceSystem = "official-api",
                sourceDetail = "Congress.gov and GovInfo",
                events = events
            )
            conn.commit()
            logger.info(
                "Normalized official data batch={} imported={} skipped={}",
                result.batchId,
                result.imported,
                result.skipped
            )
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

private fun envInt(name: String, defaultValue: Int): Int = env(name)?.toIntOrNull() ?: defaultValue

private fun defaultGovInfoStartDateTime(): String = LocalDate.now().minusDays(14).toString() + "T00:00:00Z"
