package com.publicrecord.storage.repositories

import com.publicrecord.common.models.SourceCitation
import com.publicrecord.common.models.SourceRegistryEntry
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

    fun search(citationType: String?, sourceQuality: String?, query: String?, limit: Int = 100): List<SourceCitation> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT sc.*, sr.name AS source_name, sr.source_type
                    FROM source_citations sc
                    LEFT JOIN source_registry sr ON sr.id = sc.source_id
                    WHERE (? IS NULL OR sc.citation_type = ?)
                      AND (? IS NULL OR sc.source_quality = ?)
                      AND (? IS NULL OR sc.title ILIKE ? OR sc.url ILIKE ? OR sr.name ILIKE ?)
                    ORDER BY sc.retrieved_at DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, citationType)
                    stmt.setString(2, citationType)
                    stmt.setString(3, sourceQuality)
                    stmt.setString(4, sourceQuality)
                    stmt.setString(5, search)
                    stmt.setString(6, search)
                    stmt.setString(7, search)
                    stmt.setString(8, search)
                    stmt.setInt(9, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<SourceCitation>()
                    while (rs.next()) results.add(mapCitation(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to search citations: {}", e.message, e)
            emptyList()
        }
    }

    fun findSources(sourceType: String?, query: String?, limit: Int = 100): List<SourceRegistryEntry> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT *
                    FROM source_registry
                    WHERE (? IS NULL OR source_type = ?)
                      AND (? IS NULL OR name ILIKE ? OR homepage_url ILIKE ? OR owning_entity ILIKE ?)
                    ORDER BY name ASC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, sourceType)
                    stmt.setString(2, sourceType)
                    stmt.setString(3, search)
                    stmt.setString(4, search)
                    stmt.setString(5, search)
                    stmt.setString(6, search)
                    stmt.setInt(7, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<SourceRegistryEntry>()
                    while (rs.next()) {
                        results.add(
                            SourceRegistryEntry(
                                id = rs.getObject("id") as UUID,
                                name = rs.getString("name"),
                                sourceType = rs.getString("source_type"),
                                homepageUrl = rs.getString("homepage_url"),
                                owningEntity = rs.getString("owning_entity"),
                                reputationScore = rs.getBigDecimal("reputation_score")?.toDouble(),
                                notes = rs.getString("notes"),
                                createdAt = rs.getObject("created_at", LocalDateTime::class.java),
                                updatedAt = rs.getObject("updated_at", LocalDateTime::class.java)
                            )
                        )
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find source registry entries: {}", e.message, e)
            emptyList()
        }
    }

    fun countByTarget(citationType: String, targetId: UUID): Int {
        return try {
            dbConfig.getConnection().use { conn ->
                conn.prepareStatement("SELECT COUNT(*) FROM source_citations WHERE citation_type = ? AND target_id = ?").use { stmt ->
                    stmt.setString(1, citationType)
                    stmt.setObject(2, targetId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) rs.getInt(1) else 0
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to count citations type={} targetId={}: {}", citationType, targetId, e.message, e)
            0
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
