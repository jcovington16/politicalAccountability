package com.publicrecord.storage.repositories

import com.publicrecord.common.models.VotingRecord
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class VotingRecordRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(VotingRecordRepository::class.java)

    fun findByPoliticianId(politicianId: UUID, limit: Int = 100): List<VotingRecord> {
        return find("politician_id", politicianId, limit)
    }

    fun findByBillId(billId: UUID, limit: Int = 100): List<VotingRecord> {
        return find("bill_id", billId, limit)
    }

    private fun find(column: String, id: UUID, limit: Int): List<VotingRecord> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM voting_records
                    WHERE $column = ?
                    ORDER BY vote_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    stmt.setInt(2, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<VotingRecord>()
                    while (rs.next()) results.add(mapVotingRecord(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find voting records by {}={}: {}", column, id, e.message, e)
            emptyList()
        }
    }

    private fun mapVotingRecord(rs: ResultSet): VotingRecord {
        return VotingRecord(
            id = rs.getObject("id") as UUID,
            politicianId = rs.getObject("politician_id") as UUID,
            billId = rs.getObject("bill_id") as UUID,
            voteType = rs.getString("vote_type"),
            voteDate = rs.getObject("vote_date", LocalDate::class.java)
        )
    }
}
