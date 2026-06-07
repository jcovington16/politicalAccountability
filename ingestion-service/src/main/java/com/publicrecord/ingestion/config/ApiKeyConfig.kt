package com.publicrecord.ingestion.config

/**
 * API keys are read from environment variables only. Do not pass them to
 * frontend apps, log them, or commit them to source control.
 */
data class ApiKeyConfig(
    val congressApiKey: String? = env("CONGRESS_API_KEY"),
    val govInfoApiKey: String? = env("GOVINFO_API_KEY"),
    val openStatesApiKey: String? = env("OPENSTATES_API_KEY"),
    val fecApiKey: String? = env("FEC_API_KEY"),
    val googleCivicApiKey: String? = env("GOOGLE_CIVIC_API_KEY"),
    val courtListenerApiKey: String? = env("COURTLISTENER_API_KEY"),
    val newsApiKey: String? = env("NEWSAPI_API_KEY"),
    val guardianApiKey: String? = env("GUARDIAN_API_KEY"),
    val youtubeApiKey: String? = env("YOUTUBE_API_KEY"),
    val xBearerToken: String? = env("X_BEARER_TOKEN")
) {
    fun requireCongressApiKey(): String {
        return congressApiKey ?: error("CONGRESS_API_KEY is required for Congress.gov ingestion")
    }

    fun requireGovInfoApiKey(): String {
        return govInfoApiKey ?: error("GOVINFO_API_KEY is required for GovInfo ingestion")
    }
}

private fun env(name: String): String? {
    return System.getenv(name)?.trim()?.takeIf { it.isNotBlank() }
}
