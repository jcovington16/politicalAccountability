package com.publicrecord.storage.repositories

import com.publicrecord.common.models.JoinedVotingRecord
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

    fun findJoinedByPoliticianId(politicianId: UUID, limit: Int = 100): List<JoinedVotingRecord> {
        return findJoined("vr.politician_id", politicianId, limit)
    }

    fun findJoinedByBillId(billId: UUID, limit: Int = 100): List<JoinedVotingRecord> {
        return findJoined("vr.bill_id", billId, limit)
    }

    fun searchJoined(query: String?, limit: Int = 100): List<JoinedVotingRecord> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT
                        vr.id,
                        vr.politician_id,
                        concat_ws(' ', p.first_name, p.last_name) AS politician_name,
                        p.party,
                        p.state,
                        vr.bill_id,
                        b.bill_number,
                        b.title AS bill_title,
                        b.bill_url,
                        vr.vote_type,
                        vr.vote_date
                    FROM voting_records vr
                    JOIN politicians p ON p.id = vr.politician_id
                    JOIN bills b ON b.id = vr.bill_id
                    WHERE (? IS NULL
                        OR concat_ws(' ', p.first_name, p.last_name) ILIKE ?
                        OR b.bill_number ILIKE ?
                        OR b.title ILIKE ?
                        OR vr.vote_type ILIKE ?)
                    ORDER BY vr.vote_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, search)
                    stmt.setString(2, search)
                    stmt.setString(3, search)
                    stmt.setString(4, search)
                    stmt.setString(5, search)
                    stmt.setInt(6, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<JoinedVotingRecord>()
                    while (rs.next()) results.add(mapJoinedVotingRecord(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to search joined voting records: {}", e.message, e)
            emptyList()
        }
    }

    private fun findJoined(column: String, id: UUID, limit: Int): List<JoinedVotingRecord> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT
                        vr.id,
                        vr.politician_id,
                        concat_ws(' ', p.first_name, p.last_name) AS politician_name,
                        p.party,
                        p.state,
                        vr.bill_id,
                        b.bill_number,
                        b.title AS bill_title,
                        b.bill_url,
                        vr.vote_type,
                        vr.vote_date
                    FROM voting_records vr
                    JOIN politicians p ON p.id = vr.politician_id
                    JOIN bills b ON b.id = vr.bill_id
                    WHERE $column = ?
                    ORDER BY vr.vote_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    stmt.setInt(2, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<JoinedVotingRecord>()
                    while (rs.next()) results.add(mapJoinedVotingRecord(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find joined voting records by {}={}: {}", column, id, e.message, e)
            emptyList()
        }
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

    private fun mapJoinedVotingRecord(rs: ResultSet): JoinedVotingRecord {
        return JoinedVotingRecord(
            id = rs.getObject("id") as UUID,
            politicianId = rs.getObject("politician_id") as UUID,
            politicianName = rs.getString("politician_name"),
            party = rs.getString("party"),
            state = rs.getString("state"),
            billId = rs.getObject("bill_id") as UUID,
            billNumber = rs.getString("bill_number"),
            billTitle = rs.getString("bill_title"),
            billUrl = rs.getString("bill_url"),
            voteType = rs.getString("vote_type"),
            voteDate = rs.getObject("vote_date", LocalDate::class.java)
        )
    }
}
