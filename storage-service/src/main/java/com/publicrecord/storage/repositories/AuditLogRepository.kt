package com.publicrecord.storage.repositories

import com.publicrecord.common.models.AuditLogEntry
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class AuditLogRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(AuditLogRepository::class.java)

    fun append(
        actorType: String,
        action: String,
        targetType: String,
        actorId: UUID? = null,
        targetId: UUID? = null,
        sourceSystem: String? = null,
        importBatchId: UUID? = null,
        requestId: String? = null,
        metadata: String = "{}"
    ): Boolean {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    INSERT INTO audit_log
                    (actor_type, actor_id, action, target_type, target_id, source_system, import_batch_id, request_id, metadata)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, actorType)
                    stmt.setObject(2, actorId)
                    stmt.setString(3, action)
                    stmt.setString(4, targetType)
                    stmt.setObject(5, targetId)
                    stmt.setString(6, sourceSystem)
                    stmt.setObject(7, importBatchId)
                    stmt.setString(8, requestId)
                    stmt.setString(9, metadata)
                    stmt.executeUpdate() == 1
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to append audit log action={} targetType={}: {}", action, targetType, e.message, e)
            false
        }
    }

    fun findRecent(limit: Int = 100): List<AuditLogEntry> {
        return try {
            dbConfig.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?").use { stmt ->
                    stmt.setInt(1, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<AuditLogEntry>()
                    while (rs.next()) results.add(mapEntry(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find audit log entries: {}", e.message, e)
            emptyList()
        }
    }

    private fun mapEntry(rs: ResultSet): AuditLogEntry {
        return AuditLogEntry(
            id = rs.getObject("id") as UUID,
            actorType = rs.getString("actor_type"),
            actorId = rs.getObject("actor_id") as UUID?,
            action = rs.getString("action"),
            targetType = rs.getString("target_type"),
            targetId = rs.getObject("target_id") as UUID?,
            sourceSystem = rs.getString("source_system"),
            importBatchId = rs.getObject("import_batch_id") as UUID?,
            requestId = rs.getString("request_id"),
            metadata = rs.getString("metadata"),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java)
        )
    }
}
