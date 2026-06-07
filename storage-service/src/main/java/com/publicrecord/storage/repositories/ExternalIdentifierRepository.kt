package com.publicrecord.storage.repositories

import com.publicrecord.common.models.ExternalIdentifier
import com.publicrecord.common.models.SocialAccount
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.UUID

class ExternalIdentifierRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(ExternalIdentifierRepository::class.java)

    fun upsertExternalIdentifier(
        entityType: String,
        entityId: UUID,
        sourceSystem: String,
        externalId: String,
        sourceUrl: String?,
        confidence: BigDecimal?,
        metadataJson: String = "{}"
    ): ExternalIdentifier? {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    INSERT INTO external_identifiers
                        (entity_type, entity_id, source_system, external_id, source_url, confidence, metadata)
                    VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (entity_type, source_system, external_id) DO UPDATE SET
                        entity_id = EXCLUDED.entity_id,
                        source_url = COALESCE(EXCLUDED.source_url, external_identifiers.source_url),
                        confidence = COALESCE(EXCLUDED.confidence, external_identifiers.confidence),
                        metadata = external_identifiers.metadata || EXCLUDED.metadata,
                        updated_at = CURRENT_TIMESTAMP
                    RETURNING *
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, entityType)
                    stmt.setObject(2, entityId)
                    stmt.setString(3, sourceSystem)
                    stmt.setString(4, externalId)
                    stmt.setString(5, sourceUrl)
                    stmt.setBigDecimal(6, confidence)
                    stmt.setString(7, metadataJson)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapExternalIdentifier(rs) else null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to upsert external identifier {}:{} {}", sourceSystem, externalId, e.message, e)
            null
        }
    }

    fun findBySource(entityType: String, sourceSystem: String, externalId: String): ExternalIdentifier? {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT *
                    FROM external_identifiers
                    WHERE entity_type = ? AND source_system = ? AND external_id = ?
                    LIMIT 1
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, entityType)
                    stmt.setString(2, sourceSystem)
                    stmt.setString(3, externalId)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapExternalIdentifier(rs) else null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find external identifier {}:{} {}", sourceSystem, externalId, e.message, e)
            null
        }
    }

    fun findByEntity(entityType: String, entityId: UUID, limit: Int = 50): List<ExternalIdentifier> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT *
                    FROM external_identifiers
                    WHERE entity_type = ? AND entity_id = ?
                    ORDER BY confidence DESC NULLS LAST, updated_at DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, entityType)
                    stmt.setObject(2, entityId)
                    stmt.setInt(3, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<ExternalIdentifier>()
                    while (rs.next()) results.add(mapExternalIdentifier(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find external identifiers entityType={} entityId={}: {}", entityType, entityId, e.message, e)
            emptyList()
        }
    }

    fun upsertSocialAccount(
        politicianId: UUID,
        platform: String,
        handle: String,
        accountUrl: String,
        displayName: String?,
        verificationStatus: String,
        sourceCitationId: UUID?,
        confidence: BigDecimal,
        metadataJson: String = "{}"
    ): SocialAccount? {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    INSERT INTO social_accounts
                        (politician_id, platform, handle, account_url, display_name, verification_status, source_citation_id, confidence, metadata)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (platform, handle) DO UPDATE SET
                        politician_id = EXCLUDED.politician_id,
                        account_url = EXCLUDED.account_url,
                        display_name = COALESCE(EXCLUDED.display_name, social_accounts.display_name),
                        verification_status = EXCLUDED.verification_status,
                        source_citation_id = COALESCE(EXCLUDED.source_citation_id, social_accounts.source_citation_id),
                        confidence = GREATEST(EXCLUDED.confidence, social_accounts.confidence),
                        metadata = social_accounts.metadata || EXCLUDED.metadata,
                        updated_at = CURRENT_TIMESTAMP
                    RETURNING *
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setString(2, platform)
                    stmt.setString(3, normalizeHandle(handle))
                    stmt.setString(4, accountUrl)
                    stmt.setString(5, displayName)
                    stmt.setString(6, verificationStatus)
                    stmt.setObject(7, sourceCitationId)
                    stmt.setBigDecimal(8, confidence)
                    stmt.setString(9, metadataJson)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapSocialAccount(rs) else null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to upsert social account {}:{} {}", platform, handle, e.message, e)
            null
        }
    }

    fun findSocialAccountsByPolitician(politicianId: UUID, limit: Int = 50): List<SocialAccount> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT *
                    FROM social_accounts
                    WHERE politician_id = ?
                    ORDER BY confidence DESC, platform, handle
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<SocialAccount>()
                    while (rs.next()) results.add(mapSocialAccount(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find social accounts politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    private fun normalizeHandle(handle: String): String = handle.trim().removePrefix("@").lowercase()

    private fun mapExternalIdentifier(rs: ResultSet): ExternalIdentifier = ExternalIdentifier(
        id = rs.getObject("id") as UUID,
        entityType = rs.getString("entity_type"),
        entityId = rs.getObject("entity_id") as UUID,
        sourceSystem = rs.getString("source_system"),
        externalId = rs.getString("external_id"),
        sourceUrl = rs.getString("source_url"),
        confidence = rs.getBigDecimal("confidence"),
        metadata = rs.getString("metadata"),
        createdAt = rs.getObject("created_at", LocalDateTime::class.java),
        updatedAt = rs.getObject("updated_at", LocalDateTime::class.java)
    )

    private fun mapSocialAccount(rs: ResultSet): SocialAccount = SocialAccount(
        id = rs.getObject("id") as UUID,
        politicianId = rs.getObject("politician_id") as UUID,
        platform = rs.getString("platform"),
        handle = rs.getString("handle"),
        accountUrl = rs.getString("account_url"),
        displayName = rs.getString("display_name"),
        verificationStatus = rs.getString("verification_status"),
        sourceCitationId = rs.getObject("source_citation_id") as UUID?,
        confidence = rs.getBigDecimal("confidence"),
        metadata = rs.getString("metadata"),
        createdAt = rs.getObject("created_at", LocalDateTime::class.java),
        updatedAt = rs.getObject("updated_at", LocalDateTime::class.java)
    )
}
