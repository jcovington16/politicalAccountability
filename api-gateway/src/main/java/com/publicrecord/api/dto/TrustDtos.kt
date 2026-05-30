package com.publicrecord.api.dto

import com.publicrecord.common.trust.InformationType
import com.publicrecord.common.trust.SourceQuality
import java.time.LocalDate

data class TrustScoreRequest(
    val informationType: InformationType?,
    val sourceQuality: SourceQuality?,
    val citationCount: Int = 0,
    val publishedDate: LocalDate? = null
)
