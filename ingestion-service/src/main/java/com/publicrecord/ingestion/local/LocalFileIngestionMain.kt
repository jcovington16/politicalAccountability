package com.publicrecord.ingestion.local

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.commons.csv.CSVFormat
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger("LocalFileIngestion")

fun main(args: Array<String>) {
    val inputDir = File(args.firstOrNull() ?: env("INGEST_INPUT_DIR", "data/ingestion"))
    val databaseUrl = env("DATABASE_URL", "jdbc:postgresql://localhost:5432/political_data")
    val databaseUser = env("DATABASE_USER", "postgres")
    val databasePassword = env("DATABASE_PASSWORD", "postgres")
    val searchUrl = env("ELASTICSEARCH_URL", env("OPENSEARCH_URL", "http://localhost:9200"))

    require(inputDir.exists() && inputDir.isDirectory) {
        "Input directory does not exist: ${inputDir.absolutePath}"
    }

    DriverManager.getConnection(databaseUrl, databaseUser, databasePassword).use { conn ->
        conn.autoCommit = false
        SearchIndexer(searchUrl).use { indexer ->
            val pipeline = LocalFileIngestionPipeline(conn, indexer)
            val result = pipeline.importDirectory(inputDir)
            conn.commit()
            logger.info(
                "Import complete: politicians={}, bills={}, votes={}, news={}, skipped={}",
                result.politicians,
                result.bills,
                result.votes,
                result.newsArticles,
                result.skipped
            )
        }
    }
}

class LocalFileIngestionPipeline(
    private val conn: Connection,
    private val indexer: SearchIndexer
) {
    fun importDirectory(inputDir: File): ImportResult {
        var result = ImportResult()
        result += importPoliticians(inputDir.resolveAny("politicians"))
        result += importBills(inputDir.resolveAny("bills"))
        result += importVotes(inputDir.resolveAny("votes"))
        result += importNewsArticles(inputDir.resolveAny("news_articles") ?: inputDir.resolveAny("news"))
        return result
    }

    private fun importPoliticians(file: File?): ImportResult {
        if (file == null) return ImportResult()
        var imported = 0
        var skipped = 0

        readRows(file).forEach { row ->
            val normalized = normalizeKeys(row)
            val firstName = normalized["first_name"] ?: normalized["firstname"]
            val lastName = normalized["last_name"] ?: normalized["lastname"]
            val fullName = normalized["full_name"] ?: normalized["name"]

            val names = splitName(firstName, lastName, fullName)
            val startDate = parseDate(normalized["start_date"]) ?: LocalDate.of(1900, 1, 1)
            val id = parseUuid(normalized["id"]) ?: stableUuid("politician:${names.first}:${names.second}:${normalized["state"]}")

            if (names.first.isBlank() || names.second.isBlank()) {
                skipped++
                logger.warn("Skipping politician row without usable name: {}", row)
                return@forEach
            }

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
                    biography = EXCLUDED.biography,
                    profile_image_url = EXCLUDED.profile_image_url,
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    updated_at = CURRENT_TIMESTAMP
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.setString(2, names.first)
                stmt.setString(3, names.second)
                stmt.setString(4, normalized["party"])
                stmt.setString(5, normalized["state"]?.uppercase())
                stmt.setString(6, normalized["office"])
                stmt.setString(7, normalized["biography"])
                stmt.setString(8, normalized["profile_image_url"])
                stmt.setObject(9, startDate)
                stmt.setObject(10, parseDate(normalized["end_date"]))
                stmt.executeUpdate()
            }

            indexer.index("politicians", id.toString(), mapOf(
                "id" to id.toString(),
                "fullName" to "${names.first} ${names.second}",
                "party" to normalized["party"],
                "state" to normalized["state"]?.uppercase(),
                "office" to normalized["office"],
                "biography" to normalized["biography"]
            ))
            imported++
        }

        return ImportResult(politicians = imported, skipped = skipped)
    }

    private fun importBills(file: File?): ImportResult {
        if (file == null) return ImportResult()
        var imported = 0
        var skipped = 0

        readRows(file).forEach { row ->
            val normalized = normalizeKeys(row)
            val billNumber = normalized["bill_number"] ?: normalized["number"]
            val title = normalized["title"]
            val introducedDate = parseDate(normalized["introduced_date"])

            if (billNumber.isNullOrBlank() || title.isNullOrBlank() || introducedDate == null) {
                skipped++
                logger.warn("Skipping bill row with missing bill_number/title/introduced_date: {}", row)
                return@forEach
            }

            val id = parseUuid(normalized["id"]) ?: stableUuid("bill:$billNumber")
            val introducedBy = parseUuid(normalized["introduced_by"] ?: normalized["introduced_by_id"])
            val status = normalizeBillStatus(normalized["status"])

            val sql = """
                INSERT INTO bills
                (id, bill_number, title, description, introduced_by, status, introduced_date, last_action_date, bill_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (bill_number) DO UPDATE SET
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    introduced_by = EXCLUDED.introduced_by,
                    status = EXCLUDED.status,
                    introduced_date = EXCLUDED.introduced_date,
                    last_action_date = EXCLUDED.last_action_date,
                    bill_url = EXCLUDED.bill_url
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.setString(2, billNumber.trim().uppercase())
                stmt.setString(3, title.trim())
                stmt.setString(4, normalized["description"])
                stmt.setObject(5, introducedBy)
                stmt.setString(6, status)
                stmt.setObject(7, introducedDate)
                stmt.setObject(8, parseDate(normalized["last_action_date"]))
                stmt.setString(9, normalized["bill_url"] ?: normalized["url"])
                stmt.executeUpdate()
            }

            indexer.index("bills", id.toString(), mapOf(
                "id" to id.toString(),
                "billNumber" to billNumber.trim().uppercase(),
                "title" to title,
                "description" to normalized["description"],
                "status" to status,
                "introducedDate" to introducedDate.toString()
            ))
            imported++
        }

        return ImportResult(bills = imported, skipped = skipped)
    }

    private fun importVotes(file: File?): ImportResult {
        if (file == null) return ImportResult()
        var imported = 0
        var skipped = 0

        readRows(file).forEach { row ->
            val normalized = normalizeKeys(row)
            val politicianId = parseUuid(normalized["politician_id"])
            val billId = parseUuid(normalized["bill_id"])
            val voteType = normalizeVoteType(normalized["vote_type"] ?: normalized["vote"])
            val voteDate = parseDate(normalized["vote_date"])

            if (politicianId == null || billId == null || voteType == null || voteDate == null) {
                skipped++
                logger.warn("Skipping vote row with missing politician_id/bill_id/vote_type/vote_date: {}", row)
                return@forEach
            }

            val id = parseUuid(normalized["id"]) ?: stableUuid("vote:$politicianId:$billId:$voteDate")
            val sql = """
                INSERT INTO voting_records
                (id, politician_id, bill_id, vote_type, vote_date)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    politician_id = EXCLUDED.politician_id,
                    bill_id = EXCLUDED.bill_id,
                    vote_type = EXCLUDED.vote_type,
                    vote_date = EXCLUDED.vote_date
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.setObject(2, politicianId)
                stmt.setObject(3, billId)
                stmt.setString(4, voteType)
                stmt.setObject(5, voteDate)
                stmt.executeUpdate()
            }

            indexer.index("votes", id.toString(), mapOf(
                "id" to id.toString(),
                "politicianId" to politicianId.toString(),
                "billId" to billId.toString(),
                "voteType" to voteType,
                "voteDate" to voteDate.toString()
            ))
            imported++
        }

        return ImportResult(votes = imported, skipped = skipped)
    }

    private fun importNewsArticles(file: File?): ImportResult {
        if (file == null) return ImportResult()
        var imported = 0
        var skipped = 0

        readRows(file).forEach { row ->
            val normalized = normalizeKeys(row)
            val title = normalized["title"]
            val source = normalized["source"]
            val url = normalized["url"] ?: normalized["source_url"]
            val content = normalized["content"] ?: normalized["text_body"]

            if (title.isNullOrBlank() || source.isNullOrBlank() || url.isNullOrBlank() || content.isNullOrBlank()) {
                skipped++
                logger.warn("Skipping news row with missing title/source/url/content: {}", row)
                return@forEach
            }

            val id = parseUuid(normalized["id"]) ?: stableUuid("news:$url")
            val politicianId = parseUuid(normalized["politician_id"])
            val publishedAt = parseDateTime(normalized["published_date"] ?: normalized["published_at"])

            val sql = """
                INSERT INTO news_articles
                (id, politician_id, title, source, published_date, url, content)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (url) DO UPDATE SET
                    politician_id = EXCLUDED.politician_id,
                    title = EXCLUDED.title,
                    source = EXCLUDED.source,
                    published_date = EXCLUDED.published_date,
                    content = EXCLUDED.content
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, id)
                stmt.setObject(2, politicianId)
                stmt.setString(3, title.trim())
                stmt.setString(4, source.trim())
                stmt.setObject(5, publishedAt)
                stmt.setString(6, url.trim())
                stmt.setString(7, content)
                stmt.executeUpdate()
            }

            indexer.index("news-articles", id.toString(), mapOf(
                "id" to id.toString(),
                "politicianId" to politicianId?.toString(),
                "title" to title,
                "source" to source,
                "publishedDate" to publishedAt?.toString(),
                "url" to url,
                "content" to content
            ))
            imported++
        }

        return ImportResult(newsArticles = imported, skipped = skipped)
    }
}

class SearchIndexer(searchUrl: String) : AutoCloseable {
    private val restClient = RestClient.builder(httpHost(searchUrl)).build()
    private val transport = RestClientTransport(restClient, JacksonJsonpMapper())
    private val client = ElasticsearchClient(transport)

    fun index(index: String, id: String, document: Map<String, Any?>) {
        try {
            val request = IndexRequest.Builder<Map<String, Any?>>()
                .index(index)
                .id(id)
                .document(document)
                .build()
            client.index(request)
        } catch (e: Exception) {
            logger.warn("Search indexing failed for index={} id={}: {}", index, id, e.message)
        }
    }

    override fun close() {
        transport.close()
    }

    private fun httpHost(url: String): HttpHost {
        val uri = URI(url)
        return HttpHost(uri.host, if (uri.port > 0) uri.port else 9200, uri.scheme ?: "http")
    }
}

data class ImportResult(
    val politicians: Int = 0,
    val bills: Int = 0,
    val votes: Int = 0,
    val newsArticles: Int = 0,
    val skipped: Int = 0
) {
    operator fun plus(other: ImportResult): ImportResult {
        return ImportResult(
            politicians = politicians + other.politicians,
            bills = bills + other.bills,
            votes = votes + other.votes,
            newsArticles = newsArticles + other.newsArticles,
            skipped = skipped + other.skipped
        )
    }
}

private val mapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

private fun readRows(file: File): List<Map<String, String?>> {
    return when (file.extension.lowercase()) {
        "csv" -> file.reader().use { reader ->
            CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
                .parse(reader)
                .map { record -> record.toMap() }
        }
        "json" -> mapper.readValue(file, List::class.java)
            .map { row ->
                @Suppress("UNCHECKED_CAST")
                (row as Map<String, Any?>).mapValues { it.value?.toString() }
            }
        else -> emptyList()
    }
}

private fun File.resolveAny(baseName: String): File? {
    return listOf("$baseName.csv", "$baseName.json")
        .map { File(this, it) }
        .firstOrNull { it.exists() && it.isFile }
}

private fun normalizeKeys(row: Map<String, String?>): Map<String, String?> {
    return row.mapKeys { (key, _) ->
        key.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }.mapValues { (_, value) -> value?.trim()?.takeIf { it.isNotBlank() } }
}

private fun splitName(firstName: String?, lastName: String?, fullName: String?): Pair<String, String> {
    if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
        return Pair(firstName.orEmpty().trim(), lastName.orEmpty().trim())
    }
    val parts = fullName.orEmpty().trim().split(Regex("\\s+"))
    if (parts.size < 2) return Pair(parts.firstOrNull().orEmpty(), "")
    return Pair(parts.first(), parts.drop(1).joinToString(" "))
}

private fun normalizeBillStatus(value: String?): String {
    return when (value?.trim()?.lowercase()) {
        "passed" -> "Passed"
        "failed" -> "Failed"
        "vetoed" -> "Vetoed"
        else -> "Pending"
    }
}

private fun normalizeVoteType(value: String?): String? {
    return when (value?.trim()?.uppercase()) {
        "YEA", "YES", "AYE" -> "YEA"
        "NAY", "NO" -> "NAY"
        "ABSTAIN", "PRESENT" -> "ABSTAIN"
        else -> null
    }
}

private fun parseUuid(value: String?): UUID? {
    return try {
        value?.takeIf { it.isNotBlank() }?.let(UUID::fromString)
    } catch (_: Exception) {
        null
    }
}

private fun stableUuid(input: String): UUID {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    digest[6] = (digest[6].toInt() and 0x0f or 0x40).toByte()
    digest[8] = (digest[8].toInt() and 0x3f or 0x80).toByte()
    return UUID.nameUUIDFromBytes(digest.copyOfRange(0, 16))
}

private fun parseDate(value: String?): LocalDate? {
    return try {
        value?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
    } catch (_: Exception) {
        null
    }
}

private fun parseDateTime(value: String?): LocalDateTime? {
    if (value.isNullOrBlank()) return null
    return try {
        LocalDateTime.parse(value)
    } catch (_: Exception) {
        parseDate(value)?.atStartOfDay()
    }
}

private fun env(name: String, default: String): String {
    return System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
}
