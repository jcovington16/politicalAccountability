package com.publicrecord.api.dto

data class SearchResponse(
    val query: String,
    val total: Int,
    val groups: List<SearchGroup>
)

data class SearchGroup(
    val type: String,
    val label: String,
    val results: List<SearchResult>
)

data class SearchResult(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val url: String? = null,
    val source: String? = null,
    val date: String? = null,
    val trustContext: String? = null,
    val reviewWarnings: List<String> = emptyList()
)
