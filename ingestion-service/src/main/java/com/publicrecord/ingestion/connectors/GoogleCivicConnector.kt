package com.publicrecord.ingestion.connectors

import com.fasterxml.jackson.databind.JsonNode
import com.publicrecord.common.events.ContentEventJson

class GoogleCivicConnector(
    private val apiKey: String,
    private val address: String,
    private val baseUrl: String = "https://www.googleapis.com/civicinfo/v2",
    private val httpClient: ConnectorHttpClient = JavaConnectorHttpClient()
) {
    init {
        require(apiKey.isNotBlank()) { "Google Civic API key is required" }
        require(address.isNotBlank()) { "Google Civic address is required" }
    }

    fun fetchRepresentatives(): List<GoogleCivicOfficial> {
        val url = "$baseUrl/representatives?address=${address.urlEncode()}&key=${apiKey.urlEncode()}"
        val root = ContentEventJson.mapper.readTree(httpClient.get(url))
        val offices = root.path("offices").takeIf { it.isArray } ?: return emptyList()
        val officials = root.path("officials").takeIf { it.isArray } ?: return emptyList()
        val results = mutableListOf<GoogleCivicOfficial>()

        offices.forEach { office ->
            val officeName = office.text("name") ?: "Elected official"
            office.path("officialIndices").takeIf { it.isArray }?.forEach officialLoop@{ indexNode ->
                val official = officials.get(indexNode.asInt())
                val name = official.text("name") ?: return@officialLoop
                results.add(
                    GoogleCivicOfficial(
                        externalId = "$officeName:$name",
                        name = name,
                        party = official.text("party"),
                        office = officeName,
                        state = null,
                        sourceUrl = official.path("urls").takeIf { it.isArray }?.firstOrNull()?.asText()
                            ?: "https://developers.google.com/civic-information",
                        phones = official.path("phones").takeIf { it.isArray }?.map { it.asText() }.orEmpty(),
                        emails = official.path("emails").takeIf { it.isArray }?.map { it.asText() }.orEmpty()
                    )
                )
            }
        }

        return results
    }
}

data class GoogleCivicOfficial(
    val externalId: String,
    val name: String,
    val party: String?,
    val office: String,
    val state: String?,
    val sourceUrl: String,
    val phones: List<String>,
    val emails: List<String>
)

private fun JsonNode.text(field: String): String? {
    return path(field).takeIf { !it.isMissingNode && !it.isNull }?.asText()?.takeIf { it.isNotBlank() }
}
