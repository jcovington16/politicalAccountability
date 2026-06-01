package com.publicrecord.storage.repositories

import com.publicrecord.common.models.Bill
import com.publicrecord.common.models.BillAction
import com.publicrecord.storage.config.DatabaseConfig
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

class BillRepository(private val dbConfig: DatabaseConfig) {
    private val logger = LoggerFactory.getLogger(BillRepository::class.java)

    fun save(bill: Bill): Boolean {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    INSERT INTO bills
                    (id, bill_number, title, description, introduced_by, status, introduced_date, last_action_date, bill_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (bill_number) DO UPDATE SET
                        title = EXCLUDED.title,
                        description = COALESCE(EXCLUDED.description, bills.description),
                        introduced_by = COALESCE(EXCLUDED.introduced_by, bills.introduced_by),
                        status = EXCLUDED.status,
                        introduced_date = LEAST(bills.introduced_date, EXCLUDED.introduced_date),
                        last_action_date = GREATEST(COALESCE(bills.last_action_date, EXCLUDED.last_action_date), EXCLUDED.last_action_date),
                        bill_url = COALESCE(EXCLUDED.bill_url, bills.bill_url)
                """.trimIndent()

                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, bill.id)
                    stmt.setString(2, bill.billNumber)
                    stmt.setString(3, bill.title)
                    stmt.setString(4, bill.description)
                    stmt.setObject(5, bill.introducedBy)
                    stmt.setString(6, bill.status)
                    stmt.setObject(7, bill.introducedDate)
                    stmt.setObject(8, bill.lastActionDate)
                    stmt.setString(9, bill.billUrl)
                    stmt.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to save bill {}: {}", bill.billNumber, e.message, e)
            false
        }
    }

    fun findById(id: UUID): Bill? {
        return try {
            dbConfig.getConnection().use { conn ->
                conn.prepareStatement("SELECT * FROM bills WHERE id = ?").use { stmt ->
                    stmt.setObject(1, id)
                    val rs = stmt.executeQuery()
                    if (rs.next()) mapBill(rs) else null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find bill {}: {}", id, e.message, e)
            null
        }
    }

    fun findByIntroducedBy(politicianId: UUID, limit: Int = 100): List<Bill> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM bills
                    WHERE introduced_by = ?
                    ORDER BY COALESCE(last_action_date, introduced_date) DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, politicianId)
                    stmt.setInt(2, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Bill>()
                    while (rs.next()) results.add(mapBill(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find sponsored bills politicianId={}: {}", politicianId, e.message, e)
            emptyList()
        }
    }

    fun search(query: String?, status: String?, limit: Int = 50): List<Bill> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM bills
                    WHERE (? IS NULL OR title ILIKE ? OR bill_number ILIKE ? OR description ILIKE ? OR bill_url ILIKE ?)
                      AND (? IS NULL OR status = ?)
                    ORDER BY COALESCE(last_action_date, introduced_date) DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    val search = query?.takeIf { it.isNotBlank() }?.let { "%$it%" }
                    stmt.setString(1, search)
                    stmt.setString(2, search)
                    stmt.setString(3, search)
                    stmt.setString(4, search)
                    stmt.setString(5, search)
                    stmt.setString(6, status)
                    stmt.setString(7, status)
                    stmt.setInt(8, limit.coerceIn(1, 100))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<Bill>()
                    while (rs.next()) results.add(mapBill(rs))
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to search bills: {}", e.message, e)
            emptyList()
        }
    }

    fun findActions(billId: UUID, limit: Int = 100): List<BillAction> {
        return try {
            dbConfig.getConnection().use { conn ->
                val sql = """
                    SELECT * FROM bill_actions
                    WHERE bill_id = ?
                    ORDER BY action_date DESC
                    LIMIT ?
                """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setObject(1, billId)
                    stmt.setInt(2, limit.coerceIn(1, 250))
                    val rs = stmt.executeQuery()
                    val results = mutableListOf<BillAction>()
                    while (rs.next()) {
                        results.add(
                            BillAction(
                                id = rs.getObject("id") as UUID,
                                billId = rs.getObject("bill_id") as UUID,
                                actionDate = rs.getObject("action_date", LocalDate::class.java),
                                actionText = rs.getString("action_text"),
                                sourceCitationId = rs.getObject("source_citation_id") as UUID?
                            )
                        )
                    }
                    results
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to find bill actions billId={}: {}", billId, e.message, e)
            emptyList()
        }
    }

    private fun mapBill(rs: ResultSet): Bill {
        return Bill(
            id = rs.getObject("id") as UUID,
            billNumber = rs.getString("bill_number"),
            title = rs.getString("title"),
            description = rs.getString("description"),
            introducedBy = rs.getObject("introduced_by") as UUID?,
            status = rs.getString("status"),
            introducedDate = rs.getObject("introduced_date", LocalDate::class.java),
            lastActionDate = rs.getObject("last_action_date", LocalDate::class.java),
            billUrl = rs.getString("bill_url")
        )
    }
}
