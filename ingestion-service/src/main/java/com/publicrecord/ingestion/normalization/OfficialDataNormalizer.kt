package com.publicrecord.ingestion.normalization

import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.sql.Connection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OfficialDataNormalizer(private val conn: Connection) {
    private val logger = LoggerFactory.getLogger(OfficialDataNormalizer::class.java)

    fun normalize(sourceSystem: String, sourceDetail: String?, events: List<RawContentItem>): NormalizeResult {
        val batchId = createBatch(sourceSystem, sourceDetail)
        var imported = 0
        var skipped = 0

        for (event in events) {
            try {
                val targetId = when (event.source) {
                    "Congress.gov" -> normalizeCongressBill(event)
                    "GovInfo" -> normalizeGovInfoDocument(event)
                    else -> null
                }

                if (targetId == null) {
                    skipped++
                    recordRow(batchId, event.id, null, null, "SKIPPED", "Unsupported raw event source", event)
                } else {
                    imported++
                    recordRow(batchId, event.id, "BILL", targetId, "IMPORTED", "Normalized official source event", event)
                }
            } catch (e: Exception) {
                skipped++
                logger.warn("Failed to normalize event id={}: {}", event.id, e.message)
                recordRow(batchId, event.id, null, null, "FAILED", e.message ?: "Normalization failed", event)
            }
        }

        completeBatch(batchId, events.size, imported, skipped)
        return NormalizeResult(batchId, imported, skipped)
    }

    private fun normalizeCongressBill(event: RawContentItem): UUID {
        val billNumber = event.metadata["billNumber"]?.toString()?.uppercase() ?: parseBillNumber(event.title)
        val title = event.title.substringAfter(": ", event.title)
        val billId = stableUuid("bill:$billNumber")
        val actionDate = parseDate(event.metadata["latestActionDate"]?.toString()) ?: parseDate(event.publishedDate) ?: LocalDate.now()
        val actionText = event.metadata["latestActionText"]?.toString() ?: event.textBody ?: "Updated from Congress.gov"

        upsertBill(
            id = billId,
            billNumber = billNumber,
            title = title,
            description = event.textBody,
            status = "Pending",
            introducedDate = actionDate,
            lastActionDate = actionDate,
            billUrl = event.sourceUrl
        )
        val citationId = upsertCitation("Congress.gov", "OFFICIAL_RECORD", "BILL", billId, event.title, event.sourceUrl, actionDate)
        upsertBillAction(billId, actionDate, actionText, citationId)
        return billId
    }

    private fun normalizeGovInfoDocument(event: RawContentItem): UUID? {
        val packageId = event.metadata["packageId"]?.toString() ?: event.id.removePrefix("govinfo:package:")
        val billNumber = parseBillNumberFromGovInfoPackage(packageId) ?: return null
        val billId = stableUuid("bill:$billNumber")
        val issuedDate = parseDate(event.metadata["dateIssued"]?.toString()) ?: parseDate(event.publishedDate) ?: LocalDate.now()

        upsertBill(
            id = billId,
            billNumber = billNumber,
            title = event.title,
            description = event.textBody,
            status = "Pending",
            introducedDate = issuedDate,
            lastActionDate = issuedDate,
            billUrl = event.sourceUrl
        )
        upsertCitation("GovInfo", "OFFICIAL_RECORD", "BILL", billId, event.title, event.sourceUrl, issuedDate)
        return billId
    }

    private fun upsertBill(
        id: UUID,
        billNumber: String,
        title: String,
        description: String?,
        status: String,
        introducedDate: LocalDate,
        lastActionDate: LocalDate,
        billUrl: String
    ) {
        val sql = """
            INSERT INTO bills (id, bill_number, title, description, status, introduced_date, last_action_date, bill_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (bill_number) DO UPDATE SET
                title = EXCLUDED.title,
                description = COALESCE(EXCLUDED.description, bills.description),
                status = COALESCE(bills.status, EXCLUDED.status),
                introduced_date = LEAST(bills.introduced_date, EXCLUDED.introduced_date),
                last_action_date = GREATEST(COALESCE(bills.last_action_date, EXCLUDED.last_action_date), EXCLUDED.last_action_date),
                bill_url = COALESCE(EXCLUDED.bill_url, bills.bill_url)
        """.trimIndent()
        conn.prepareStatement(sql).use { stmt ->
            stmt.setObject(1, id)
            stmt.setString(2, billNumber)
            stmt.setString(3, title)
            stmt.setString(4, description)
            stmt.setString(5, status)
            stmt.setObject(6, introducedDate)
            stmt.setObject(7, lastActionDate)
            stmt.setString(8, billUrl)
            stmt.executeUpdate()
        }
    }

    private fun upsertCitation(
        sourceName: String,
        sourceQuality: String,
        citationType: String,
        targetId: UUID,
        title: String,
        url: String,
        publishedDate: LocalDate
    ): UUID {
        val sourceId = stableUuid("source:$sourceName")
        val citationId = stableUuid("citation:$citationType:$targetId:$url")

        conn.prepareStatement(
            """
            INSERT INTO source_registry (id, name, source_type, homepage_url, reputation_score)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (name, homepage_url) DO UPDATE SET
                source_type = EXCLUDED.source_type,
                reputation_score = EXCLUDED.reputation_score,
                updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, sourceId)
            stmt.setString(2, sourceName)
            stmt.setString(3, sourceQuality)
            stmt.setString(4, homepageFor(sourceName))
            stmt.setBigDecimal(5, java.math.BigDecimal("99.00"))
            stmt.executeUpdate()
        }

        conn.prepareStatement(
            """
            INSERT INTO source_citations
            (id, source_id, citation_type, target_id, title, url, published_at, source_quality, confidence)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (citation_type, target_id, url) DO UPDATE SET
                title = EXCLUDED.title,
                source_quality = EXCLUDED.source_quality,
                confidence = EXCLUDED.confidence
            """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, citationId)
            stmt.setObject(2, sourceId)
            stmt.setString(3, citationType)
            stmt.setObject(4, targetId)
            stmt.setString(5, title)
            stmt.setString(6, url)
            stmt.setObject(7, publishedDate.atStartOfDay())
            stmt.setString(8, sourceQuality)
            stmt.setBigDecimal(9, java.math.BigDecimal("99.00"))
            stmt.executeUpdate()
        }
        return citationId
    }

    private fun upsertBillAction(billId: UUID, actionDate: LocalDate, actionText: String, citationId: UUID) {
        conn.prepareStatement(
            """
            INSERT INTO bill_actions (bill_id, action_date, action_text, source_citation_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (bill_id, action_date, action_text) DO UPDATE SET
                source_citation_id = EXCLUDED.source_citation_id
            """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, billId)
            stmt.setObject(2, actionDate)
            stmt.setString(3, actionText)
            stmt.setObject(4, citationId)
            stmt.executeUpdate()
        }
    }

    private fun createBatch(sourceSystem: String, sourceDetail: String?): UUID {
        val id = UUID.randomUUID()
        conn.prepareStatement(
            "INSERT INTO import_batches (id, source_system, source_detail) VALUES (?, ?, ?)"
        ).use { stmt ->
            stmt.setObject(1, id)
            stmt.setString(2, sourceSystem)
            stmt.setString(3, sourceDetail)
            stmt.executeUpdate()
        }
        return id
    }

    private fun completeBatch(id: UUID, seen: Int, imported: Int, skipped: Int) {
        conn.prepareStatement(
            """
            UPDATE import_batches
            SET status = ?, completed_at = CURRENT_TIMESTAMP, records_seen = ?, records_imported = ?, records_skipped = ?
            WHERE id = ?
            """.trimIndent()
        ).use { stmt ->
            stmt.setString(1, if (skipped > 0 && imported == 0) "FAILED" else "COMPLETED")
            stmt.setInt(2, seen)
            stmt.setInt(3, imported)
            stmt.setInt(4, skipped)
            stmt.setObject(5, id)
            stmt.executeUpdate()
        }
    }

    private fun recordRow(batchId: UUID, sourceRecordId: String, targetType: String?, targetId: UUID?, status: String, message: String, event: RawContentItem) {
        conn.prepareStatement(
            """
            INSERT INTO import_row_results
            (import_batch_id, source_record_id, target_type, target_id, status, message, row_payload)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
            """.trimIndent()
        ).use { stmt ->
            stmt.setObject(1, batchId)
            stmt.setString(2, sourceRecordId)
            stmt.setString(3, targetType)
            stmt.setObject(4, targetId)
            stmt.setString(5, status)
            stmt.setString(6, message)
            stmt.setString(7, ContentEventJson.toJson(event))
            stmt.executeUpdate()
        }
    }

    private fun homepageFor(sourceName: String): String {
        return when (sourceName) {
            "Congress.gov" -> "https://www.congress.gov"
            "GovInfo" -> "https://www.govinfo.gov"
            else -> "https://example.invalid"
        }
    }

    private fun parseBillNumber(title: String): String {
        return Regex("""\b([A-Z]+)-?(\d+)\b""").find(title.uppercase())?.let {
            "${it.groupValues[1]}-${it.groupValues[2]}"
        } ?: title.take(40).uppercase()
    }

    private fun parseBillNumberFromGovInfoPackage(packageId: String): String? {
        val match = Regex("""BILLS-\d+([a-z]+)(\d+).*""", RegexOption.IGNORE_CASE).matchEntire(packageId)
        return match?.let { "${it.groupValues[1].uppercase()}-${it.groupValues[2]}" }
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        return try {
            LocalDate.parse(value.take(10))
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(value).toLocalDate()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun stableUuid(input: String): UUID {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        digest[6] = (digest[6].toInt() and 0x0f or 0x40).toByte()
        digest[8] = (digest[8].toInt() and 0x3f or 0x80).toByte()
        return UUID.nameUUIDFromBytes(digest.copyOfRange(0, 16))
    }
}

data class NormalizeResult(val batchId: UUID, val imported: Int, val skipped: Int)
