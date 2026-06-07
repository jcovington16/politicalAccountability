package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson
import com.publicrecord.ingestion.ContentConnector
import com.publicrecord.ingestion.RawContentItem
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Uses the official X API to discover public posts.
 *
 * X is useful for accountability when an account is verified as belonging to a
 * politician, campaign, committee, or office. This connector only emits raw
 * public post candidates. A later normalization/review stage should attach the
 * post to a verified social account, create a source citation, and decide
 * whether it is a direct quote, claim, or general content item.
 */
class XPostConnector @JvmOverloads constructor(
    private val bearerToken: String,
    private val queries: List<String>,
    private val maxResults: Int = 10,
    private val baseUrl: String = "https://api.x.com/2/tweets/search/recent",
    private val httpClient: HeaderAwareConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(XPostConnector::class.java)

    init {
        require(bearerToken.isNotBlank()) { "X bearer token is required" }
        require(queries.isNotEmpty()) { "At least one X query is required" }
        require(maxResults in 10..100) { "X maxResults must be between 10 and 100" }
    }

    override fun getName(): String = "XPostConnector(queries=${queries.size})"

    override fun fetch(): List<RawContentItem> {
        return queries.flatMap { query ->
            val url = "$baseUrl?query=${query.urlEncode()}" +
                "&max_results=$maxResults" +
                "&tweet.fields=author_id,created_at,conversation_id,public_metrics,entities" +
                "&expansions=author_id" +
                "&user.fields=username,name,verified"
            logger.info("Fetching X posts for query='{}'", query)
            val root = ContentEventJson.mapper.readTree(
                httpClient.get(url, mapOf("Authorization" to "Bearer $bearerToken"))
            )
            val users = root.path("includes").path("users").associateBy { it.path("id").asText("") }
            root.path("data").mapNotNull { post -> mapPost(query, post, users) }
        }.distinctBy { it.sourceUrl }
    }

    private fun mapPost(query: String, post: JsonNode, users: Map<String, JsonNode>): RawContentItem? {
        val postId = post.path("id").asText("").trim()
        val text = post.path("text").asText("").trim()
        if (postId.isBlank() || text.isBlank()) return null

        val author = users[post.path("author_id").asText("")]
        val username = author?.path("username")?.asText(null)
        val sourceUrl = if (!username.isNullOrBlank()) "https://x.com/$username/status/$postId" else "https://x.com/i/web/status/$postId"

        return RawContentItem(
            id = stableSocialId("x:$postId"),
            title = text.take(120),
            contentType = "social_post",
            textBody = text,
            mediaUrl = null,
            sourceUrl = sourceUrl,
            publishedDate = normalizeSocialDate(post.path("created_at").asText(null)),
            source = "X",
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "SOCIAL_MEDIA",
                "platform" to "X",
                "postId" to postId,
                "authorId" to post.path("author_id").asText(null),
                "username" to username,
                "displayName" to author?.path("name")?.asText(null),
                "verified" to author?.path("verified")?.takeUnless { it.isMissingNode || it.isNull }?.asBoolean(),
                "query" to query,
                "sourceCollectionRole" to "PUBLIC_SOCIAL_DISCOVERY",
                "accountabilityUse" to "Capture public posts for citation review and direct-quote classification"
            ).filterValues { it != null }
        )
    }
}

/**
 * Uses Bluesky's public AppView search endpoint to discover public posts.
 *
 * Bluesky is a good early social source because public search can be used
 * without handling private account data. We still treat results as citation
 * candidates until account identity and context are reviewed.
 */
class BlueskyPostConnector @JvmOverloads constructor(
    private val queries: List<String>,
    private val limit: Int = 25,
    private val baseUrl: String = "https://public.api.bsky.app/xrpc/app.bsky.feed.searchPosts",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) : ContentConnector {
    private val logger = LoggerFactory.getLogger(BlueskyPostConnector::class.java)

    init {
        require(queries.isNotEmpty()) { "At least one Bluesky query is required" }
        require(limit in 1..100) { "Bluesky limit must be between 1 and 100" }
    }

    override fun getName(): String = "BlueskyPostConnector(queries=${queries.size})"

    override fun fetch(): List<RawContentItem> {
        return queries.flatMap { query ->
            val url = "$baseUrl?q=${query.urlEncode()}&limit=$limit"
            logger.info("Fetching Bluesky posts for query='{}'", query)
            val root = ContentEventJson.mapper.readTree(httpClient.get(url))
            root.path("posts").mapNotNull { post -> mapPost(query, post) }
        }.distinctBy { it.sourceUrl }
    }

    private fun mapPost(query: String, post: JsonNode): RawContentItem? {
        val uri = post.path("uri").asText("").trim()
        val record = post.path("record")
        val text = record.path("text").asText("").trim()
        val author = post.path("author")
        val handle = author.path("handle").asText("").trim()
        val rkey = uri.substringAfterLast("/").takeIf { it.isNotBlank() }
        if (uri.isBlank() || text.isBlank() || handle.isBlank() || rkey.isNullOrBlank()) return null

        val sourceUrl = "https://bsky.app/profile/$handle/post/$rkey"
        return RawContentItem(
            id = stableSocialId("bluesky:$uri"),
            title = text.take(120),
            contentType = "social_post",
            textBody = text,
            mediaUrl = null,
            sourceUrl = sourceUrl,
            publishedDate = normalizeSocialDate(record.path("createdAt").asText(null)),
            source = "Bluesky",
            politicianName = null,
            metadata = mapOf(
                "sourceQuality" to "SOCIAL_MEDIA",
                "platform" to "BLUESKY",
                "postUri" to uri,
                "postCid" to post.path("cid").asText(null),
                "handle" to handle,
                "displayName" to author.path("displayName").asText(null),
                "did" to author.path("did").asText(null),
                "query" to query,
                "sourceCollectionRole" to "PUBLIC_SOCIAL_DISCOVERY",
                "accountabilityUse" to "Capture public posts for citation review and direct-quote classification"
            ).filterValues { it != null }
        )
    }
}

private fun normalizeSocialDate(value: String?): String {
    if (value.isNullOrBlank()) return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    return runCatching { OffsetDateTime.parse(value).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        .getOrElse { value.trim() }
}

private fun stableSocialId(value: String): String = UUID.nameUUIDFromBytes(value.toByteArray()).toString()
