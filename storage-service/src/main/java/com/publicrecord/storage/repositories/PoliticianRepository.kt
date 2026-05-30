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
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                // Check if already exists
                val checkSql = "SELECT id FROM politicians WHERE id = ?"
                val existing = conn.prepareStatement(checkSql).use { stmt ->
                    stmt.setObject(1, politician.id)
                    stmt.executeQuery().next()
                }

                val sql = if (existing) {
                    // Update
                    """
                        UPDATE politicians SET 
                            first_name = ?, last_name = ?, party = ?, state = ?, 
                            office = ?, biography = ?, profile_image_url = ?, 
                            start_date = ?, end_date = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                    """.trimIndent()
                } else {
                    // Insert
                    """
                        INSERT INTO politicians 
                        (id, first_name, last_name, party, state, office, biography, 
                         profile_image_url, start_date, end_date)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                }

                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, politician.firstName)
                    stmt.setString(2, politician.lastName)
                    stmt.setString(3, politician.party)
                    stmt.setString(4, politician.state)
                    stmt.setString(5, politician.office)
                    stmt.setString(6, politician.biography)
                    stmt.setString(7, politician.profileImageUrl)
                    stmt.setObject(8, politician.startDate)
                    stmt.setObject(9, politician.endDate)
                    if (existing) {
                        stmt.setObject(10, politician.id)
                    } else {
                        stmt.setObject(10, politician.id)
                    }
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
                    WHERE first_name ILIKE ? OR last_name ILIKE ?
                    ORDER BY first_name, last_name
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    val searchTerm = "%$nameQuery%"
                    stmt.setString(1, searchTerm)
                    stmt.setString(2, searchTerm)
                    stmt.setInt(3, limit)
                    
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