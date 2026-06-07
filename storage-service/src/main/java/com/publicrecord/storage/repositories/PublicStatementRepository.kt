package com.publicrecord.storage.repositories

import com.publicrecord.common.models.PublicStatement
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class PublicStatementRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(PublicStatementRepository::class.java)

    fun findByPoliticianId(politicianId: UUID, limit: Int = 100): List<PublicStatement> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM public_statements
                    WHERE politician_id = ?
                    ORDER BY statement_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<PublicStatement>()
                    while (rs.next()) results.add(mapStatement(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find statements politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    fun search(query: String?, statementType: String?, limit: Int = 100): List<PublicStatement> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM public_statements
                    WHERE (? IS NULL OR title ILIKE ? OR body ILIKE ? OR quote ILIKE ? OR venue ILIKE ?)
                      AND (? IS NULL OR statement_type = ?)
                    ORDER BY statement_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, search)
                    stmt.setString(2, search)
                    stmt.setString(3, search)
                    stmt.setString(4, search)
                    stmt.setString(5, search)
                    stmt.setString(6, statementType)
                    stmt.setString(7, statementType)
                    stmt.setInt(8, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<PublicStatement>()
                    while (rs.next()) results.add(mapStatement(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to search statements: {}", e.message, e)
            emptyList()
        }
    }

    private fun mapStatement(rs: ResultSet): PublicStatement {
        return PublicStatement(
            id = rs.getObject("id") as UUID,
            politicianId = rs.getObject("politician_id") as UUID?,
            statementType = rs.getString("statement_type"),
            title = rs.getString("title"),
            body = rs.getString("body"),
            quote = rs.getString("quote"),
            venue = rs.getString("venue"),
            statementDate = rs.getObject("statement_date", LocalDateTime::class.java),
            sourceCitationId = rs.getObject("source_citation_id") as UUID?,
            confidence = rs.getBigDecimal("confidence")?.toDouble()
        )
    }
}
