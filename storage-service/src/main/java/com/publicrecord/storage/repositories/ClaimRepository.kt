package com.publicrecord.storage.repositories

import com.publicrecord.common.models.Claim
import com.publicrecord.common.models.FactCheck
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class ClaimRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(ClaimRepository::class.java)

    fun findByPoliticianId(politicianId: UUID, claimType: String?, status: String?, limit: Int = 100): List<Claim> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT c.*,
                           COUNT(sc.id) AS citation_count,
                           MAX(sc.published_at) AS latest_citation_at
                    FROM claims c
                    LEFT JOIN source_citations sc ON sc.citation_type = 'CLAIM' AND sc.target_id = c.id
                    WHERE c.politician_id = ?
                      AND (? IS NULL OR c.claim_type = ?)
                      AND (? IS NULL OR c.status = ?)
                    GROUP BY c.id
                    ORDER BY COALESCE(c.last_reviewed_at, c.first_seen_at, c.created_at) DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setString(2, claimType)
                    stmt.setString(3, claimType)
                    stmt.setString(4, status)
                    stmt.setString(5, status)
                    stmt.setInt(6, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Claim>()
                    while (rs.next()) results.add(mapClaim(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find claims politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    fun search(query: String?, claimType: String?, status: String?, limit: Int = 100): List<Claim> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT c.*,
                           COUNT(sc.id) AS citation_count,
                           MAX(sc.published_at) AS latest_citation_at
                    FROM claims c
                    LEFT JOIN source_citations sc ON sc.citation_type = 'CLAIM' AND sc.target_id = c.id
                    WHERE (? IS NULL OR c.claim_text ILIKE ?)
                      AND (? IS NULL OR c.claim_type = ?)
                      AND (? IS NULL OR c.status = ?)
                    GROUP BY c.id
                    ORDER BY COALESCE(c.last_reviewed_at, c.first_seen_at, c.created_at) DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, search)
                    stmt.setString(2, search)
                    stmt.setString(3, claimType)
                    stmt.setString(4, claimType)
                    stmt.setString(5, status)
                    stmt.setString(6, status)
                    stmt.setInt(7, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Claim>()
                    while (rs.next()) results.add(mapClaim(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to search claims: {}", e.message, e)
            emptyList()
        }
    }

    fun findFactChecks(claimId: UUID): List<FactCheck> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM fact_checks
                    WHERE claim_id = ?
                    ORDER BY checked_at DESC
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, claimId)
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<FactCheck>()
                    while (rs.next()) {
                        results.add(
                            FactCheck(
                                id = rs.getObject("id") as UUID,
                                claimId = rs.getObject("claim_id") as UUID,
                                rating = rs.getString("rating"),
                                summary = rs.getString("summary"),
                                checkedBy = rs.getString("checked_by"),
                                checkedAt = rs.getObject("checked_at", LocalDateTime::class.java),
                                sourceCitationId = rs.getObject("source_citation_id") as UUID?
                            )
                        )
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find fact checks claimId={}: {}", claimId, e.message, e)
            emptyList()
        }
    }

    private fun mapClaim(rs: ResultSet): Claim {
        return Claim(
            id = rs.getObject("id") as UUID,
            politicianId = rs.getObject("politician_id") as UUID?,
            statementId = rs.getObject("statement_id") as UUID?,
            claimText = rs.getString("claim_text"),
            claimType = rs.getString("claim_type"),
            status = rs.getString("status"),
            confidence = rs.getBigDecimal("confidence")?.toDouble(),
            firstSeenAt = rs.getObject("first_seen_at", LocalDateTime::class.java),
            lastReviewedAt = rs.getObject("last_reviewed_at", LocalDateTime::class.java),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java),
            updatedAt = rs.getObject("updated_at", LocalDateTime::class.java),
            citationCount = rs.getInt("citation_count"),
            latestCitationAt = rs.getObject("latest_citation_at", LocalDateTime::class.java)
        )
    }
}
