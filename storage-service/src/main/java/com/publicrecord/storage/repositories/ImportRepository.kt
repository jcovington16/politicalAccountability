package com.publicrecord.storage.repositories

import com.publicrecord.common.models.ImportBatch
import com.publicrecord.common.models.ImportRowResult
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class ImportRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(ImportRepository::class.java)

    fun findBatches(status: String?, limit: Int = 50): List<ImportBatch> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM import_batches
                    WHERE (? IS NULL OR status = ?)
                    ORDER BY started_at DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, status)
                    stmt.setString(2, status)
                    stmt.setInt(3, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ImportBatch>()
                    while (rs.next()) results.add(mapBatch(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find import batches: {}", e.message, e)
            emptyList()
        }
    }

    fun findBatch(id: UUID): ImportBatch? {
        return try {
            dbConfig.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM import_batches WHERE id = ?").use { stmt ->
                    stmt.setObject(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapBatch(rs) else null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find import batch {}: {}", id, e.message, e)
            null
        }
    }

    fun findRows(importBatchId: UUID, status: String?, limit: Int = 100): List<ImportRowResult> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM import_row_results
                    WHERE import_batch_id = ?
                      AND (? IS NULL OR status = ?)
                    ORDER BY created_at DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, importBatchId)
                    stmt.setString(2, status)
                    stmt.setString(3, status)
                    stmt.setInt(4, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ImportRowResult>()
                    while (rs.next()) results.add(mapRow(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find import rows batchId={}: {}", importBatchId, e.message, e)
            emptyList()
        }
    }

    private fun mapBatch(rs: ResultSet): ImportBatch {
        return ImportBatch(
            id = rs.getObject("id") as UUID,
            sourceSystem = rs.getString("source_system"),
            sourceDetail = rs.getString("source_detail"),
            status = rs.getString("status"),
            startedAt = rs.getObject("started_at", LocalDateTime::class.java),
            completedAt = rs.getObject("completed_at", LocalDateTime::class.java),
            recordsSeen = rs.getInt("records_seen"),
            recordsImported = rs.getInt("records_imported"),
            recordsSkipped = rs.getInt("records_skipped"),
            errorMessage = rs.getString("error_message"),
            sourceChecksum = rs.getString("source_checksum"),
            metadata = rs.getString("metadata")
        )
    }

    private fun mapRow(rs: ResultSet): ImportRowResult {
        return ImportRowResult(
            id = rs.getObject("id") as UUID,
            importBatchId = rs.getObject("import_batch_id") as UUID,
            sourceRecordId = rs.getString("source_record_id"),
            targetType = rs.getString("target_type"),
            targetId = rs.getObject("target_id") as UUID?,
            status = rs.getString("status"),
            message = rs.getString("message"),
            rowPayload = rs.getString("row_payload"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java)
        )
    }
}
