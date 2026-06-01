package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson

class CongressMemberConnector(
    private val apiKey: String,
    private val stateCode: String? = null,
    private val currentMember: Boolean? = null,
    private val limit: Int = 250,
    private val baseUrl: String = "https://api.congress.gov/v3",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) {
    init {
        require(apiKey.isNotBlank()) { "Congress.gov API key is required" }
        require(limit in 1..250) { "Congress.gov member limit must be between 1 and 250" }
    }

    fun fetchMembers(): List<CongressMember> {
        val path = if (stateCode.isNullOrBlank()) "$baseUrl/member" else "$baseUrl/member/${stateCode.uppercase()}"
        val current = currentMember?.let { "&currentMember=$it" } ?: ""
        val url = "$path?format=json&limit=${limit.coerceIn(1, 250)}$current&api_key=${apiKey.urlEncode()}"
        val root = ContentEventJson.mapper.readTree(httpClient.get(url))
        val members = root.path("members")
        if (!members.isArray) return emptyList()
        return members.mapNotNull(::mapMember)
    }

    private fun mapMember(node: JsonNode): CongressMember? {
        val bioguideId = node.text("bioguideId") ?: return null
        val name = node.text("name") ?: return null
        val state = node.text("state")
        val terms = node.path("terms").path("item").takeIf { it.isArray }
        val latestTerm = terms?.lastOrNull()
        val chamber = latestTerm?.text("chamber")
        val startYear = latestTerm?.text("startYear")?.toIntOrNull()
        val endYear = latestTerm?.text("endYear")?.toIntOrNull()

        return CongressMember(
            bioguideId = bioguideId,
            name = name,
            party = node.text("partyName"),
            state = stateCode ?: state,
            office = chamber ?: "Member of Congress",
            startYear = startYear,
            endYear = endYear,
            imageUrl = node.path("depiction").text("imageUrl"),
            sourceUrl = node.text("url") ?: "$baseUrl/member/${bioguideId.urlEncode()}?format=json"
        )
    }
}

data class CongressMember(
    val bioguideId: String,
    val name: String,
    val party: String?,
    val state: String?,
    val office: String,
    val startYear: Int?,
    val endYear: Int?,
    val imageUrl: String?,
    val sourceUrl: String
)

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}
