package com.publicrecord.storage.repositories

import com.publicrecord.common.models.ContentItem
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.util.*
import java.time.LocalDateTime

/**
 * Repository for ContentItem database operations
 */
class ContentItemRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(ContentItemRepository::class.java)

    /**
     * Save or update a content item
     */
    fun save(contentItem: ContentItem): Boolean {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                // Check if already exists (by content_hash)
                val checkSql = "SELECT id FROM content_items WHERE content_hash = ?"
                val existing = conn.prepareStatement(checkSql).use { stmt ->
                    stmt.setString(1, contentItem.contentHash)
                    stmt.executeQuery().next()
                }

                if (existing) {
                    logger.info("Content item already exists: ${contentItem.contentHash}")
                    return@use true
                }

                // Insert new content item
                val insertSql = """
                    INSERT INTO content_items 
                    (id, title, content_type, text_body, media_url, published_at, 
                     content_hash, source_url, politician_id, keywords, tags, indexed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

                conn.prepareStatement(insertSql).use { stmt ->
                    stmt.setObject(1, contentItem.id)
                    stmt.setString(2, contentItem.title)
                    stmt.setString(3, contentItem.contentType)
                    stmt.setString(4, contentItem.textBody)
                    stmt.setString(5, contentItem.mediaUrl)
                    stmt.setObject(6, contentItem.publishedAt)
                    stmt.setString(7, contentItem.contentHash)
                    stmt.setString(8, contentItem.sourceUrl)
                    stmt.setObject(9, contentItem.politicianId)
                    stmt.setArray(10, conn.createArrayOf("VARCHAR", contentItem.keywords.toTypedArray()))
                    stmt.setArray(11, conn.createArrayOf("VARCHAR", contentItem.tags.toTypedArray()))
                    stmt.setObject(12, contentItem.indexedAt)

                    stmt.executeUpdate()
                    logger.info("✅ Saved content item: ${contentItem.title}")
                }

                // Save provenance metadata if available
                if (contentItem.provenance != null) {
                    saveProvenance(conn, contentItem.id, contentItem.provenance!!)
                }

                true
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to save content item: ${e.message}", e)
            false
        }
    }

    /**
     * Get content item by ID
     */
    fun findById(id: UUID): ContentItem? {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = "SELECT * FROM content_items WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        mapResultSetToContentItem(rs)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find content item: ${e.message}", e)
            null
        }
    }

    /**
     * Get all content items for a politician
     */
    fun findByPoliticianId(politicianId: UUID, limit: Int = 100, offset: Int = 0): List<ContentItem> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM content_items 
                    WHERE politician_id = ? 
                    ORDER BY published_at DESC 
                    LIMIT ? OFFSET ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit)
                    stmt.setInt(3, offset)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ContentItem>()
                    while (rs.next()) {
                        results.add(mapResultSetToContentItem(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find content items for politician: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Search content by keyword (PostgreSQL LIKE)
     */
    fun searchByKeyword(keyword: String, limit: Int = 50): List<ContentItem> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM content_items 
                    WHERE title ILIKE ? OR text_body ILIKE ?
                    ORDER BY published_at DESC
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    val searchTerm = "%$keyword%"
                    stmt.setString(1, searchTerm)
                    stmt.setString(2, searchTerm)
                    stmt.setInt(3, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ContentItem>()
                    while (rs.next()) {
                        results.add(mapResultSetToContentItem(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to search content: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get content items by date range
     */
    fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime, limit: Int = 100): List<ContentItem> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM content_items 
                    WHERE published_at BETWEEN ? AND ?
                    ORDER BY published_at DESC
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, startDate)
                    stmt.setObject(2, endDate)
                    stmt.setInt(3, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ContentItem>()
                    while (rs.next()) {
                        results.add(mapResultSetToContentItem(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find content by date range: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Get content items by content type
     */
    fun findByContentType(contentType: String, limit: Int = 100): List<ContentItem> {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = """
                    SELECT * FROM content_items 
                    WHERE content_type = ?
                    ORDER BY published_at DESC
                    LIMIT ?
                """.trimIndent()
                
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, contentType)
                    stmt.setInt(2, limit)
                    
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ContentItem>()
                    while (rs.next()) {
                        results.add(mapResultSetToContentItem(rs))
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to find content by type: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Delete content item
     */
    fun delete(id: UUID): Boolean {
        return try {
            val connection = dbConfig.getConnection()
            connection.use { conn ->
                val sql = "DELETE FROM content_items WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, id)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to delete content item: ${e.message}", e)
            false
        }
    }

    /**
     * Helper: Save provenance metadata
     */
    private fun saveProvenance(conn: java.sql.Connection, contentItemId: UUID, provenance: com.publicrecord.common.models.ProvenanceMetadata) {
        try {
            val sql = """
                INSERT INTO provenance (id, content_item_id, source_type, extractor_version, confidence, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, UUID.randomUUID())
                stmt.setObject(2, contentItemId)
                stmt.setString(3, provenance.sourceType)
                stmt.setString(4, provenance.extractorVersion)
                stmt.setObject(5, provenance.confidence)
                stmt.setObject(6, provenance.timestamp)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to save provenance metadata: ${e.message}")
        }
    }

    /**
     * Helper: Map ResultSet to ContentItem
     */
    private fun mapResultSetToContentItem(rs: ResultSet): ContentItem {
        return ContentItem(
            id = rs.getObject("id") as UUID,
            title = rs.getString("title"),
            contentType = rs.getString("content_type"),
            textBody = rs.getString("text_body"),
            mediaUrl = rs.getString("media_url"),
            publishedAt = rs.getObject("published_at", LocalDateTime::class.java),
            contentHash = rs.getString("content_hash"),
            sourceUrl = rs.getString("source_url"),
            politicianId = rs.getObject("politician_id") as UUID,
            keywords = emptyList(), // TODO: Parse from array
            tags = emptyList(), // TODO: Parse from array
            indexedAt = rs.getObject("indexed_at", LocalDateTime::class.java)
        )
    }
}

