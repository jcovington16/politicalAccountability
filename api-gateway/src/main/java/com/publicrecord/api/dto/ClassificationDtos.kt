package com.publicrecord.api.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.publicrecord.common.classification.ClassificationInput
import com.publicrecord.common.classification.ReviewStatus

data class CivicClassificationRequest @JsonCreator constructor(
    @JsonProperty("title")
    val title: String? = null,
    @JsonProperty("text")
    val text: String?,
    @JsonProperty("sourceQuality")
    val sourceQuality: String? = null,
    @JsonProperty("citationCount")
    val citationCount: Int = 0,
    @JsonProperty("directQuote")
    val directQuote: Boolean = false,
    @JsonProperty("officialRecord")
    val officialRecord: Boolean = false,
    @JsonProperty("reviewStatus")
    val reviewStatus: ReviewStatus? = null
) {
    fun toInput(): ClassificationInput = ClassificationInput(
        title = title,
        text = text.orEmpty(),
        sourceQuality = sourceQuality,
        citationCount = citationCount,
        isDirectQuote = directQuote,
        isOfficialRecord = officialRecord,
        reviewerStatus = reviewStatus
    )
}
