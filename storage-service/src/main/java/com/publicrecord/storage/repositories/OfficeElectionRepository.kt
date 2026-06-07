package com.publicrecord.storage.repositories

import com.publicrecord.common.models.ElectionCandidateItem
import com.publicrecord.common.models.ElectionHistoryItem
import com.publicrecord.common.models.OfficeHistoryItem
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class OfficeElectionRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(OfficeElectionRepository::class.java)

    fun findOfficeHistory(politicianId: UUID, limit: Int = 50): List<OfficeHistoryItem> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT
                        po.office_id,
                        po.title,
                        o.branch,
                        o.office_level,
                        o.jurisdiction,
                        o.state,
                        o.district,
                        o.seat_identifier,
                        po.start_date,
                        po.end_date,
                        po.is_current,
                        po.source_url
                    FROM politician_offices po
                    JOIN offices o ON o.id = po.office_id
                    WHERE po.politician_id = ?
                    ORDER BY po.is_current DESC, po.start_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<OfficeHistoryItem>()
                    while (rs.next()) results.add(mapOfficeHistory(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find office history politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    fun findElectionHistoryForPolitician(politicianId: UUID, limit: Int = 20): List<ElectionHistoryItem> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT DISTINCT e.*
                    FROM election_candidates ec
                    JOIN elections e ON e.id = ec.election_id
                    WHERE ec.politician_id = ?
                    ORDER BY e.election_date DESC
                    LIMIT ?
                """.trimIndent()
                val electionIds = conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val ids = mutableListOf<UUID>()
                    while (rs.next()) ids.add(rs.getObject("id") as UUID)
                    ids
                }

                electionIds.mapNotNull { findElection(conn, it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to find election history politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    private fun findElection(conn: java.sql.Connection, electionId: UUID): ElectionHistoryItem? {
        val electionSql = "SELECT * FROM elections WHERE id = ?"
        val election = conn.prepareStatement(electionSql).use { stmt ->
            stmt.setObject(1, electionId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                ElectionHistoryItem(
                    electionId = rs.getObject("id") as UUID,
                    officeId = rs.getObject("office_id") as UUID,
                    electionDate = rs.getObject("election_date", LocalDate::class.java),
                    electionType = rs.getString("election_type"),
                    cycleYear = rs.getInt("cycle_year"),
                    jurisdiction = rs.getString("jurisdiction"),
                    sourceUrl = rs.getString("source_url"),
                    candidates = emptyList()
                )
            } else {
                null
            }
        } ?: return null

        val candidateSql = """
            SELECT
                ec.politician_id,
                concat_ws(' ', p.first_name, p.last_name) AS full_name,
                ec.party,
                ec.ballot_status,
                ec.result_status,
                ec.vote_total,
                ec.vote_percentage,
                ec.source_url
            FROM election_candidates ec
            JOIN politicians p ON p.id = ec.politician_id
            WHERE ec.election_id = ?
            ORDER BY ec.result_status NULLS LAST, full_name
        """.trimIndent()
        val candidates = conn.prepareStatement(candidateSql).use { stmt ->
            stmt.setObject(1, electionId)
            val rs = stmt.executeQuery()
            val results = mutableListOf<ElectionCandidateItem>()
            while (rs.next()) {
                results.add(
                    ElectionCandidateItem(
                        politicianId = rs.getObject("politician_id") as UUID,
                        fullName = rs.getString("full_name"),
                        party = rs.getString("party"),
                        ballotStatus = rs.getString("ballot_status"),
                        resultStatus = rs.getString("result_status"),
                        voteTotal = rs.getObject("vote_total") as Int?,
                        votePercentage = rs.getBigDecimal("vote_percentage"),
                        sourceUrl = rs.getString("source_url")
                    )
                )
            }
            results
        }

        return election.copy(candidates = candidates)
    }

    private fun mapOfficeHistory(rs: ResultSet): OfficeHistoryItem {
        return OfficeHistoryItem(
            officeId = rs.getObject("office_id") as UUID,
            title = rs.getString("title"),
            branch = rs.getString("branch"),
            officeLevel = rs.getString("office_level"),
            jurisdiction = rs.getString("jurisdiction"),
            state = rs.getString("state"),
            district = rs.getString("district"),
            seatIdentifier = rs.getString("seat_identifier"),
            startDate = rs.getObject("start_date", LocalDate::class.java),
            endDate = rs.getObject("end_date", LocalDate::class.java),
            isCurrent = rs.getBoolean("is_current"),
            sourceUrl = rs.getString("source_url")
        )
    }
}
