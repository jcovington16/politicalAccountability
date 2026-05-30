package com.publicrecord.common.events

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.UUID

/**
 * Shared event contract for content before it has been enriched.
 *
 * Ingestion publishes this event to Kafka, and processing consumes the same
 * type. Keeping it in common prevents the two services from silently drifting.
 */
data class RawContentEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val contentType: String,
    val textBody: String?,
    val mediaUrl: String?,
    val sourceUrl: String,
    val publishedDate: String,
    val source: String,
    val politicianName: String? = null,
    val metadata: Map<String, Any?> = emptyMap()
)

object ContentEventJson {
    val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun toJson(value: Any): String = mapper.writeValueAsString(value)

    inline fun <reified T> fromJson(json: String): T = mapper.readValue(json)
}
