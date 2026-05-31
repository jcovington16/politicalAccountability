package com.publicrecord.storage.repositories

import com.publicrecord.common.models.SourceCitation
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class SourceCitationRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(SourceCitationRepository::class.java)

    fun findByTarget(citationType: String, targetId: UUID, limit: Int = 100): List<SourceCitation> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT sc.*, sr.name AS source_name, sr.source_type
                    FROM source_citations sc
                    LEFT JOIN source_registry sr ON sr.id = sc.source_id
                    WHERE sc.citation_type = ? AND sc.target_id = ?
                    ORDER BY sc.retrieved_at DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, citationType)
                    stmt.setObject(2, targetId)
                    stmt.setInt(3, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<SourceCitation>()
                    while (rs.next()) results.add(mapCitation(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find citations type={} targetId={}: {}", citationType, targetId, e.message, e)
            emptyList()
        }
    }

    private fun mapCitation(rs: ResultSet): SourceCitation {
        return SourceCitation(
            id = rs.getObject("id") as UUID,
            sourceName = rs.getString("source_name"),
            sourceType = rs.getString("source_type"),
            citationType = rs.getString("citation_type"),
            targetId = rs.getObject("target_id") as UUID?,
            title = rs.getString("title"),
            url = rs.getString("url"),
            archiveUrl = rs.getString("archive_url"),
            publishedAt = rs.getObject("published_at", LocalDateTime::class.java),
            retrievedAt = rs.getObject("retrieved_at", LocalDateTime::class.java),
            quote = rs.getString("quote"),
            sourceQuality = rs.getString("source_quality"),
            confidence = rs.getBigDecimal("confidence")?.toDouble()
        )
    }
}
