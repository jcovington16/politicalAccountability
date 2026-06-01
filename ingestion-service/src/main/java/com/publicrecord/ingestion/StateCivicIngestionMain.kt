package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.GoogleCivicConnector
import com.publicrecord.ingestion.connectors.GoogleCivicOfficial
import com.publicrecord.ingestion.connectors.OpenStatesBill
import com.publicrecord.ingestion.connectors.OpenStatesConnector
import com.publicrecord.ingestion.connectors.OpenStatesPerson
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID

fun main() {
    val logger = LoggerFactory.getLogger("StateCivicIngestionMain")
    val keys = ApiKeyConfig()
    val databaseUrl = env("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/political_data"
    val databaseUser = env("DATABASE_USER") ?: env("DB_USERNAME") ?: "postgres"
    val databasePassword = env("DATABASE_PASSWORD") ?: env("DB_PASSWORD") ?: "postgres"

    DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).use { conn ->
        conn.autoCommit = false
        var people = 0
        var bills = 0

        keys.openStatesApiKey?.let { apiKey ->
            val connector = OpenStatesConnector(
                apiKey = apiKey,
                jurisdiction = env("OPENSTATES_JURISDICTION") ?: "ocd-jurisdiction/country:us/state:co/government",
                session = env("OPENSTATES_SESSION"),
                limit = (env("OPENSTATES_LIMIT")?.toIntOrNull() ?: 25).coerceIn(1, 100)
            )
            connector.fetchPeople().forEach {
                upsertOpenStatesPerson(conn, it)
                people++
            }
            try {
                connector.fetchBills().forEach {
                    upsertOpenStatesBill(conn, it)
                    bills++
                }
            } catch (e: Exception) {
                logger.warn("Skipping Open States bill import: {}", e.message)
            }
        } ?: logger.warn("Skipping Open States ingestion because OPENSTATES_API_KEY is not set")

        val civicAddress = env("GOOGLE_CIVIC_ADDRESS")
        if (!keys.googleCivicApiKey.isNullOrBlank() && !civicAddress.isNullOrBlank()) {
            GoogleCivicConnector(keys.googleCivicApiKey, civicAddress).fetchRepresentatives().forEach {
                upsertGoogleCivicOfficial(conn, it)
                people++
            }
        } else {
            logger.warn("Skipping Google Civic ingestion because GOOGLE_CIVIC_API_KEY or GOOGLE_CIVIC_ADDRESS is not set")
        }

        conn.commit()
        logger.info("State/civic ingestion complete: people={} bills={}", people, bills)
    }
}

private fun upsertOpenStatesPerson(conn: Connection, person: OpenStatesPerson) {
    val id = existingEntityId(conn, "POLITICIAN", "Open States", person.externalId) ?: stableUuid("openstates:person:${person.externalId}")
    val names = splitName(person.name)
    upsertPolitician(conn, id, names.first, names.second, person.party, person.state, person.office, person.sourceUrl)
    upsertExternalId(conn, "POLITICIAN", id, "Open States", person.externalId, person.sourceUrl, """{"jurisdiction":"${person.jurisdiction.jsonEscape()}"}""")
    upsertCitation(conn, "Open States", "OFFICIAL_RECORD", "OFFICE", id, person.name, person.sourceUrl)
}

private fun upsertGoogleCivicOfficial(conn: Connection, official: GoogleCivicOfficial) {
    val id = existingEntityId(conn, "POLITICIAN", "Google Civic", official.externalId) ?: stableUuid("google-civic:official:${official.externalId}")
    val names = splitName(official.name)
    upsertPolitician(conn, id, names.first, names.second, official.party, official.state, official.office, official.sourceUrl)
    upsertExternalId(conn, "POLITICIAN", id, "Google Civic", official.externalId, official.sourceUrl, """{"phones":${official.phones.toJsonArray()},"emails":${official.emails.toJsonArray()}}""")
    upsertCitation(conn, "Google Civic", "PRIMARY_SOURCE", "OFFICE", id, official.name, official.sourceUrl)
}

private fun upsertOpenStatesBill(conn: Connection, bill: OpenStatesBill) {
    val id = existingEntityId(conn, "BILL", "Open States", bill.externalId) ?: stableUuid("openstates:bill:${bill.externalId}")
    conn.prepareStatement(
        """
        INSERT INTO bills (id, bill_number, title, description, status, introduced_date, last_action_date, bill_url)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (bill_number) DO UPDATE SET
            title = EXCLUDED.title,
            description = COALESCE(EXCLUDED.description, bills.description),
            status = EXCLUDED.status,
            introduced_date = LEAST(bills.introduced_date, EXCLUDED.introduced_date),
            last_action_date = GREATEST(COALESCE(bills.last_action_date, EXCLUDED.last_action_date), EXCLUDED.last_action_date),
            bill_url = COALESCE(EXCLUDED.bill_url, bills.bill_url)
        """.trimIndent()
    ).use { stmt ->
        val introduced = parseDate(bill.introducedDate) ?: LocalDate.now()
        stmt.setObject(1, id)
        stmt.setString(2, bill.identifier.uppercase())
        stmt.setString(3, bill.title)
        stmt.setString(4, bill.description)
        stmt.setString(5, bill.status)
        stmt.setObject(6, introduced)
        stmt.setObject(7, parseDate(bill.lastActionDate) ?: introduced)
        stmt.setString(8, bill.sourceUrl)
        stmt.executeUpdate()
    }
    upsertExternalId(conn, "BILL", id, "Open States", bill.externalId, bill.sourceUrl, "{}")
    upsertCitation(conn, "Open States", "OFFICIAL_RECORD", "BILL", id, bill.title, bill.sourceUrl)
}

private fun upsertPolitician(conn: Connection, id: UUID, firstName: String, lastName: String, party: String?, state: String?, office: String, sourceUrl: String) {
    conn.prepareStatement(
        """
        INSERT INTO politicians (id, first_name, last_name, party, state, office, biography, start_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            party = COALESCE(EXCLUDED.party, politicians.party),
            state = COALESCE(EXCLUDED.state, politicians.state),
            office = EXCLUDED.office,
            biography = COALESCE(politicians.biography, EXCLUDED.biography),
            updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, id)
        stmt.setString(2, firstName)
        stmt.setString(3, lastName)
        stmt.setString(4, party)
        stmt.setString(5, state)
        stmt.setString(6, office)
        stmt.setString(7, "Imported from official civic source: $sourceUrl")
        stmt.setObject(8, LocalDate.now())
        stmt.executeUpdate()
    }
}

private fun upsertExternalId(conn: Connection, entityType: String, entityId: UUID, sourceSystem: String, externalId: String, sourceUrl: String, metadata: String) {
    conn.prepareStatement(
        """
        INSERT INTO external_identifiers (entity_type, entity_id, source_system, external_id, source_url, confidence, metadata)
        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
        ON CONFLICT (entity_type, source_system, external_id) DO UPDATE SET
            entity_id = EXCLUDED.entity_id,
            source_url = EXCLUDED.source_url,
            confidence = EXCLUDED.confidence,
            metadata = EXCLUDED.metadata,
            updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setString(1, entityType)
        stmt.setObject(2, entityId)
        stmt.setString(3, sourceSystem)
        stmt.setString(4, externalId)
        stmt.setString(5, sourceUrl)
        stmt.setBigDecimal(6, java.math.BigDecimal("95.00"))
        stmt.setString(7, metadata)
        stmt.executeUpdate()
    }
}

private fun upsertCitation(conn: Connection, sourceName: String, sourceQuality: String, citationType: String, targetId: UUID, title: String, url: String) {
    val sourceId = stableUuid("source:$sourceName")
    conn.prepareStatement(
        """
        INSERT INTO source_registry (id, name, source_type, homepage_url, reputation_score)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (name, homepage_url) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, sourceId)
        stmt.setString(2, sourceName)
        stmt.setString(3, sourceQuality)
        stmt.setString(4, if (sourceName == "Open States") "https://openstates.org" else "https://developers.google.com/civic-information")
        stmt.setBigDecimal(5, java.math.BigDecimal("95.00"))
        stmt.executeUpdate()
    }
    conn.prepareStatement(
        """
        INSERT INTO source_citations (id, source_id, citation_type, target_id, title, url, source_quality, confidence)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (citation_type, target_id, url) DO UPDATE SET title = EXCLUDED.title
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, stableUuid("citation:$citationType:$targetId:$url"))
        stmt.setObject(2, sourceId)
        stmt.setString(3, citationType)
        stmt.setObject(4, targetId)
        stmt.setString(5, title)
        stmt.setString(6, url)
        stmt.setString(7, sourceQuality)
        stmt.setBigDecimal(8, java.math.BigDecimal("95.00"))
        stmt.executeUpdate()
    }
}

private fun existingEntityId(conn: Connection, entityType: String, sourceSystem: String, externalId: String): UUID? {
    conn.prepareStatement("SELECT entity_id FROM external_identifiers WHERE entity_type = ? AND source_system = ? AND external_id = ?").use { stmt ->
        stmt.setString(1, entityType)
        stmt.setString(2, sourceSystem)
        stmt.setString(3, externalId)
        val rs = stmt.executeQuery()
        return if (rs.next()) rs.getObject("entity_id") as UUID else null
    }
}

private fun splitName(name: String): Pair<String, String> {
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size == 1) parts.first() to "Unknown" else parts.first() to parts.drop(1).joinToString(" ")
}

private fun parseDate(value: String?): LocalDate? = try {
    value?.take(10)?.let(LocalDate::parse)
} catch (_: Exception) {
    null
}

private fun stableUuid(input: String): UUID {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return UUID.nameUUIDFromBytes(digest.copyOfRange(0, 16))
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")

private fun List<String>.toJsonArray(): String = joinToString(prefix = "[", postfix = "]") { "\"${it.jsonEscape()}\"" }
