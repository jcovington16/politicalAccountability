package com.publicrecord.ingestion

import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.UUID

fun main() {
    val databaseUrl = env("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/political_data"
    val databaseUser = env("DATABASE_USER") ?: env("DB_USERNAME") ?: "postgres"
    val databasePassword = env("DATABASE_PASSWORD") ?: env("DB_PASSWORD") ?: "postgres"

    DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).use { conn ->
        conn.autoCommit = false
        recentPresidents().forEach { seed ->
            upsertPresident(conn, seed)
            upsertExternalId(conn, seed)
            upsertCitation(conn, seed)
        }
        conn.commit()
        println("Seeded ${recentPresidents().size} federal executive profiles")
    }
}

private data class PresidentSeed(
    val firstName: String,
    val lastName: String,
    val party: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val sourceUrl: String,
    val profileImageUrl: String? = null
) {
    val id: UUID = stableUuid("federal-executive:president:$firstName:$lastName:$startDate")
    val fullName: String = "$firstName $lastName"
}

private fun recentPresidents(): List<PresidentSeed> = listOf(
    PresidentSeed(
        firstName = "Donald",
        lastName = "Trump",
        party = "Republican",
        startDate = LocalDate.parse("2025-01-20"),
        endDate = null,
        sourceUrl = "https://www.whitehouse.gov/administration/donald-j-trump/"
    ),
    PresidentSeed(
        firstName = "Joe",
        lastName = "Biden",
        party = "Democratic",
        startDate = LocalDate.parse("2021-01-20"),
        endDate = LocalDate.parse("2025-01-20"),
        sourceUrl = "https://americanhistory.si.edu/explore/exhibitions/american-presidency/online/resources/list-of-presidents"
    ),
    PresidentSeed(
        firstName = "Barack",
        lastName = "Obama",
        party = "Democratic",
        startDate = LocalDate.parse("2009-01-20"),
        endDate = LocalDate.parse("2017-01-20"),
        sourceUrl = "https://americanhistory.si.edu/explore/exhibitions/american-presidency/online/resources/list-of-presidents"
    ),
    PresidentSeed(
        firstName = "George W.",
        lastName = "Bush",
        party = "Republican",
        startDate = LocalDate.parse("2001-01-20"),
        endDate = LocalDate.parse("2009-01-20"),
        sourceUrl = "https://americanhistory.si.edu/explore/exhibitions/american-presidency/online/resources/list-of-presidents"
    ),
    PresidentSeed(
        firstName = "Bill",
        lastName = "Clinton",
        party = "Democratic",
        startDate = LocalDate.parse("1993-01-20"),
        endDate = LocalDate.parse("2001-01-20"),
        sourceUrl = "https://americanhistory.si.edu/explore/exhibitions/american-presidency/online/resources/list-of-presidents"
    )
)

private fun upsertPresident(conn: Connection, seed: PresidentSeed) {
    conn.prepareStatement(
        """
        INSERT INTO politicians (id, first_name, last_name, party, state, office, biography, profile_image_url, start_date, end_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
            first_name = EXCLUDED.first_name,
            last_name = EXCLUDED.last_name,
            party = EXCLUDED.party,
            state = EXCLUDED.state,
            office = EXCLUDED.office,
            biography = COALESCE(politicians.biography, EXCLUDED.biography),
            profile_image_url = COALESCE(politicians.profile_image_url, EXCLUDED.profile_image_url),
            start_date = EXCLUDED.start_date,
            end_date = EXCLUDED.end_date,
            updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, seed.id)
        stmt.setString(2, seed.firstName)
        stmt.setString(3, seed.lastName)
        stmt.setString(4, seed.party)
        stmt.setString(5, "US")
        stmt.setString(6, "President of the United States")
        stmt.setString(7, "Seeded federal executive profile for search coverage. Enrich with official records, statements, orders, citations, and timeline events before relying on the profile for a full voter view.")
        stmt.setString(8, seed.profileImageUrl)
        stmt.setObject(9, seed.startDate)
        stmt.setObject(10, seed.endDate)
        stmt.executeUpdate()
    }
}

private fun upsertExternalId(conn: Connection, seed: PresidentSeed) {
    conn.prepareStatement(
        """
        INSERT INTO external_identifiers (entity_type, entity_id, source_system, external_id, source_url, confidence, metadata)
        VALUES ('POLITICIAN', ?, 'Federal Executive Seed', ?, ?, 90.00, ?::jsonb)
        ON CONFLICT (entity_type, source_system, external_id) DO UPDATE SET
            entity_id = EXCLUDED.entity_id,
            source_url = EXCLUDED.source_url,
            confidence = EXCLUDED.confidence,
            metadata = EXCLUDED.metadata,
            updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, seed.id)
        stmt.setString(2, "president:${seed.fullName}:${seed.startDate}")
        stmt.setString(3, seed.sourceUrl)
        stmt.setString(4, """{"branch":"EXECUTIVE","officeLevel":"FEDERAL","seeded":true}""")
        stmt.executeUpdate()
    }
}

private fun upsertCitation(conn: Connection, seed: PresidentSeed) {
    val sourceName = if (seed.sourceUrl.contains("whitehouse.gov")) "The White House" else "Smithsonian National Museum of American History"
    val sourceUrl = if (seed.sourceUrl.contains("whitehouse.gov")) "https://www.whitehouse.gov" else "https://americanhistory.si.edu"
    val sourceId = stableUuid("source:$sourceName:$sourceUrl")
    conn.prepareStatement(
        """
        INSERT INTO source_registry (id, name, source_type, homepage_url, reputation_score)
        VALUES (?, ?, 'PRIMARY_SOURCE', ?, 95.00)
        ON CONFLICT (name, homepage_url) DO UPDATE SET updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, sourceId)
        stmt.setString(2, sourceName)
        stmt.setString(3, sourceUrl)
        stmt.executeUpdate()
    }
    conn.prepareStatement(
        """
        INSERT INTO source_citations (id, source_id, citation_type, target_id, title, url, source_quality, confidence)
        VALUES (?, ?, 'OFFICE', ?, ?, ?, 'PRIMARY_SOURCE', 90.00)
        ON CONFLICT (citation_type, target_id, url) DO UPDATE SET title = EXCLUDED.title
        """.trimIndent()
    ).use { stmt ->
        stmt.setObject(1, stableUuid("citation:president:${seed.id}:${seed.sourceUrl}"))
        stmt.setObject(2, sourceId)
        stmt.setObject(3, seed.id)
        stmt.setString(4, seed.fullName)
        stmt.setString(5, seed.sourceUrl)
        stmt.executeUpdate()
    }
}

private fun stableUuid(input: String): UUID {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return UUID.nameUUIDFromBytes(digest.copyOfRange(0, 16))
}

private fun env(name: String): String? = System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
