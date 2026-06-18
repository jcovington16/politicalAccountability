package com.publicrecord.api.dto

import com.publicrecord.common.trust.InformationType
import com.publicrecord.common.trust.SourceQuality
import java.time.LocalDate

class TrustScoreRequest() {
    var informationType: InformationType? = null
    var sourceQuality: SourceQuality? = null
    var citationCount: Int = 0
    var publishedDate: LocalDate? = null

    constructor(
        informationType: InformationType?,
        sourceQuality: SourceQuality?,
        citationCount: Int = 0,
        publishedDate: LocalDate? = null
    ) : this() {
        this.informationType = informationType
        this.sourceQuality = sourceQuality
        this.citationCount = citationCount
        this.publishedDate = publishedDate
    }
}
