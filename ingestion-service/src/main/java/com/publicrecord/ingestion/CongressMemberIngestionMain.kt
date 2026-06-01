package com.publicrecord.ingestion

import com.publicrecord.ingestion.config.ApiKeyConfig
import com.publicrecord.ingestion.connectors.CongressMember
import com.publicrecord.ingestion.connectors.CongressMemberConnector
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID

fun main() {
    val logger = LoggerFactory.getLogger("CongressMemberIngestionMain")
    val apiKey = ApiKeyConfig().requireCongressApiKey()
    val databaseUrl = env("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/political_data"
    val databaseUser = env("DATABASE_USER") ?: env("DB_USERNAME") ?: "postgres"
    val databasePassword = env("DATABASE_PASSWORD") ?: env("DB_PASSWORD") ?: "postgres"

    val members = CongressMemberConnector(
        apiKey = apiKey,
        stateCode = env("CONGRESS_MEMBER_STATE"),
        currentMember = env("CONGRESS_CURRENT_MEMBER")?.toBooleanStrictOrNull(),
        limit = (env("CONGRESS_MEMBER_LIMIT")?.toIntOrNull() ?: 250).coerceIn(1, 250)
    ).fetchMembers()

    DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).use { conn ->
        conn.autoCommit = false
        members.forEach { upsertMember(conn, it) }
        conn.commit()
    }

    logger.info("Congress member ingestion complete: members={}", members.size)
}

private fun upsertMember(conn: Connection, member: CongressMember) {
    val id = existingEntityId(conn, "POLITICIAN", "Congress.gov", member.bioguideId)
        ?: stableUuid("congress-member:${member.bioguideId}")
    val names = splitCongressName(member.name)
    val startDate = LocalDate.of(member.startYear ?: 1900, 1, 1)
    val endDate = member.endYear?.let { LocalDate.of(it, 12, 31) }

    conn.prepareStatement(
        """
        INSERT INTO politicians
        (id, first_name, last_name, party, state, office, biography, profile_image_url, start_date, end_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            party = COALESCE(EXCLUDED.party, politicians.party),
            state = COALESCE(EXCLUDED.state, politicians.state),
            office = EXCLUDED.office,
            biography = COALESCE(politicians.biography, EXCLUDED.biography),
            profile_image_url = COALESCE(EXCLUDED.profile_image_url, politicians.profile_image_url),
            start_date = LEAST(politicians.start_date, EXCLUDED.start_date),
            end_date = EXCLUDED.end_date,
            updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, id)
        stmt.setString(2, names.first)
        stmt.setString(3, names.second)
        stmt.setString(4, member.party)
        stmt.setString(5, member.state?.uppercase())
        stmt.setString(6, member.office)
        stmt.setString(7, "Imported from Congress.gov member profile ${member.bioguideId}")
        stmt.setString(8, member.imageUrl)
        stmt.setObject(9, startDate)
        stmt.setObject(10, endDate)
        stmt.executeUpdate()
    }

    upsertExternalId(conn, id, member)
    upsertCitation(conn, id, member)
}

private fun upsertExternalId(conn: Connection, entityId: UUID, member: CongressMember) {
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
        stmt.setString(1, "POLITICIAN")
        stmt.setObject(2, entityId)
        stmt.setString(3, "Congress.gov")
        stmt.setString(4, member.bioguideId)
        stmt.setString(5, member.sourceUrl)
        stmt.setBigDecimal(6, java.math.BigDecimal("99.00"))
        stmt.setString(7, """{"office":"${member.office.jsonEscape()}"}""")
        stmt.executeUpdate()
    }
}

private fun upsertCitation(conn: Connection, entityId: UUID, member: CongressMember) {
    val sourceId = stableUuid("source:Congress.gov")
    conn.prepareStatement(
        """
        INSERT INTO source_registry (id, name, source_type, homepage_url, reputation_score)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (name, homepage_url) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, sourceId)
        stmt.setString(2, "Congress.gov")
        stmt.setString(3, "OFFICIAL_RECORD")
        stmt.setString(4, "https://www.congress.gov")
        stmt.setBigDecimal(5, java.math.BigDecimal("99.00"))
        stmt.executeUpdate()
    }

    conn.prepareStatement(
        """
        INSERT INTO source_citations (id, source_id, citation_type, target_id, title, url, source_quality, confidence)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (citation_type, target_id, url) DO UPDATE SET title = EXCLUDED.title
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, stableUuid("citation:OFFICE:$entityId:${member.sourceUrl}"))
        stmt.setObject(2, sourceId)
        stmt.setString(3, "OFFICE")
        stmt.setObject(4, entityId)
        stmt.setString(5, member.name)
        stmt.setString(6, member.sourceUrl)
        stmt.setString(7, "OFFICIAL_RECORD")
        stmt.setBigDecimal(8, java.math.BigDecimal("99.00"))
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

private fun splitCongressName(name: String): Pair<String, String> {
    val cleaned = name.replace(Regex("\\s+"), " ").trim()
    if (!cleaned.contains(",")) {
        val parts = cleaned.split(" ")
        return parts.first() to parts.drop(1).joinToString(" ").ifBlank { "Unknown" }
    }
    val last = cleaned.substringBefore(",").trim()
    val first = cleaned.substringAfter(",").trim().substringBefore(" ")
    return first.ifBlank { "Unknown" } to last.ifBlank { "Unknown" }
}

private fun stableUuid(input: String): UUID {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return UUID.nameUUIDFromBytes(digest.copyOfRange(0, 16))
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }

private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
