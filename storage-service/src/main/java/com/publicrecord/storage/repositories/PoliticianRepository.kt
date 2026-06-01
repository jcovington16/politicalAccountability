package com.publicrecord.storage.repositories

import com.publicrecord.common.models.Politician
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.*
import java.time.LocalDate

/**
 * Repository for Politician database operations
 */
class PoliticianRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(PoliticianRepository::class.java)

    /**
     * Save or update a politician
     */
    fun save(politician: Politician): Boolean {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    INSERT INTO politicians
                    (id, first_name, last_name, party, state, office, biography, profile_image_url, start_date, end_date)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        first_name = EXCLUDED.first_name,
                        last_name = EXCLUDED.last_name,
                        party = EXCLUDED.party,
                        state = EXCLUDED.state,
                        office = EXCLUDED.office,
                        biography = COALESCE(EXCLUDED.biography, politicians.biography),
                        profile_image_url = COALESCE(EXCLUDED.profile_image_url, politicians.profile_image_url),
                        start_date = COALESCE(EXCLUDED.start_date, politicians.start_date),
                        end_date = EXCLUDED.end_date,
                        updated_at = CURRENT_TIMESTAMP
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politician.id)
                    stmt.setString(2, politician.firstName)
                    stmt.setString(3, politician.lastName)
                    stmt.setString(4, politician.party)
                    stmt.setString(5, politician.state)
                    stmt.setString(6, politician.office)
                    stmt.setString(7, politician.biography)
                    stmt.setString(8, politician.profileImageUrl)
                    stmt.setObject(9, politician.startDate)
                    stmt.setObject(10, politician.endDate)
                    stmt.executeUpdate()
                    logger.info("✅ Saved politician: ${politician.firstName} ${politician.lastName}")
                }
                true
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to save politician: ${e.message}", e)
            false
        }
    }

    /**
     * Get politician by ID
     */
    fun findById(id: UUID): Politician? {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = "SELECT * FROM politicians WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        mapResultSetToPolitician(rs)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find politician: ${e.message}", e)
            null
        }
    }

    /**
     * Search politicians by name
     */
    fun searchByName(nameQuery: String, limit: Int = 50): List<Politician> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM politicians 
                    WHERE 
                        concat_ws(' ', first_name, last_name) ILIKE ?
                        OR first_name ILIKE ?
                        OR last_name ILIKE ?
                        OR office ILIKE ?
                        OR state ILIKE ?
                        OR party ILIKE ?
                    ORDER BY first_name, last_name
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    val searchTerm = "%$nameQuery%"
                    stmt.setString(1, searchTerm)
                    stmt.setString(2, searchTerm)
                    stmt.setString(3, searchTerm)
                    stmt.setString(4, searchTerm)
                    stmt.setString(5, searchTerm)
                    stmt.setString(6, searchTerm)
                    stmt.setInt(7, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Politician>()
                    while (rs.next()) {
                        results.add(mapResultSetToPolitician(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to search politicians: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get politicians by state
     */
    fun findByState(state: String, limit: Int = 100): List<Politician> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM politicians 
                    WHERE state = ? AND end_date IS NULL
                    ORDER BY first_name, last_name
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, state)
                    stmt.setInt(2, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Politician>()
                    while (rs.next()) {
                        results.add(mapResultSetToPolitician(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find politicians by state: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get politicians by party
     */
    fun findByParty(party: String, limit: Int = 100): List<Politician> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM politicians 
                    WHERE party = ? AND end_date IS NULL
                    ORDER BY first_name, last_name
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, party)
                    stmt.setInt(2, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Politician>()
                    while (rs.next()) {
                        results.add(mapResultSetToPolitician(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find politicians by party: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Delete politician
     */
    fun delete(id: UUID): Boolean {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = "DELETE FROM politicians WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to delete politician: ${e.message}", e)
            false
        }
    }

    /**
     * Helper: Map ResultSet to Politician
     */
    private fun mapResultSetToPolitician(rs: ResultSet): Politician {
        return Politician(
            id = rs.getObject("id") as UUID,
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            party = rs.getString("party") ?: "",
            state = rs.getString("state") ?: "",
            office = rs.getString("office") ?: "",
            biography = rs.getString("biography"),
            profileImageUrl = rs.getString("profile_image_url"),
            startDate = rs.getObject("start_date", LocalDate::class.java),
            endDate = rs.getObject("end_date", LocalDate::class.java)
        )
    }
}
