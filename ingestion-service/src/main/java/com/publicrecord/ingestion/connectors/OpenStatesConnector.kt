package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenStatesConnector(
    private val apiKey: String,
    private val jurisdiction: String,
    private val session: String? = null,
    private val limit: Int = 25,
    private val baseUrl: String = "https://v3.openstates.org"
) {
    private val client = HttpClient.newHttpClient()

    init {
        require(apiKey.isNotBlank()) { "Open States API key is required" }
        require(jurisdiction.isNotBlank()) { "Open States jurisdiction is required" }
        require(limit in 1..100) { "Open States limit must be between 1 and 100" }
    }

    fun fetchPeople(): List<OpenStatesPerson> {
        val url = "$baseUrl/people?jurisdiction=${jurisdiction.urlEncode()}&per_page=$limit"
        return resultArray(getJson(url)).mapNotNull(::mapPerson)
    }

    fun fetchBills(): List<OpenStatesBill> {
        val sessionQuery = session?.takeIf { it.isNotBlank() }?.let { "&session=${it.urlEncode()}" } ?: ""
        val url = "$baseUrl/bills?jurisdiction=${jurisdiction.urlEncode()}$sessionQuery&per_page=$limit"
        return resultArray(getJson(url)).mapNotNull(::mapBill)
    }

    private fun getJson(url: String): JsonNode {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .header("X-API-KEY", apiKey)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Open States request failed with status ${response.statusCode()}")
        }
        return ContentEventJson.mapper.readTree(response.body())
    }

    private fun resultArray(root: JsonNode): List<JsonNode> {
        val candidates = listOf(root.path("results"), root.path("data"), root.path("items"))
        return candidates.firstOrNull { it.isArray }?.toList() ?: emptyList()
    }

    private fun mapPerson(node: JsonNode): OpenStatesPerson? {
        val id = node.text("id") ?: return null
        val name = node.text("name") ?: return null
        val role = node.path("current_role")
        val jurisdictionName = node.path("jurisdiction").text("name") ?: jurisdiction
        return OpenStatesPerson(
            externalId = id,
            name = name,
            party = node.text("party") ?: role.text("party"),
            state = jurisdiction.substringAfter("state:", "").substringBefore("/").uppercase().takeIf { it.length == 2 },
            office = role.text("title") ?: role.text("org_classification") ?: "State official",
            jurisdiction = jurisdictionName,
            sourceUrl = node.text("openstates_url") ?: node.text("sources_url") ?: "$baseUrl/people/${id.urlEncode()}"
        )
    }

    private fun mapBill(node: JsonNode): OpenStatesBill? {
        val id = node.text("id") ?: return null
        val identifier = node.text("identifier") ?: return null
        val title = node.text("title") ?: return null
        val latestAction = firstObject(node.path("actions")) ?: node.path("latest_action")
        return OpenStatesBill(
            externalId = id,
            identifier = identifier,
            title = title,
            description = firstText(node.path("abstracts"), "abstract") ?: title,
            status = "Pending",
            introducedDate = firstText(node.path("actions"), "date") ?: node.text("created_at") ?: node.text("updated_at"),
            lastActionDate = latestAction.text("date") ?: node.text("updated_at"),
            sourceUrl = node.text("openstates_url") ?: firstText(node.path("sources"), "url") ?: "$baseUrl/bills/ocd-bill/${id.urlEncode()}"
        )
    }

    private fun firstObject(node: JsonNode): JsonNode? = node.takeIf { it.isArray }?.firstOrNull { it.isObject }

    private fun firstText(node: JsonNode, field: String): String? = firstObject(node)?.text(field)
}

data class OpenStatesPerson(
    val externalId: String,
    val name: String,
    val party: String?,
    val state: String?,
    val office: String,
    val jurisdiction: String,
    val sourceUrl: String
)

data class OpenStatesBill(
    val externalId: String,
    val identifier: String,
    val title: String,
    val description: String?,
    val status: String,
    val introducedDate: String?,
    val lastActionDate: String?,
    val sourceUrl: String
)

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}
