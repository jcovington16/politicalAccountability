package com.publicrecord.common.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SourceCitation(
    val id: UUID,
    val sourceName: String?,
    val sourceType: String?,
    val citationType: String,
    val targetId: UUID?,
    val title: String?,
    val url: String,
    val archiveUrl: String?,
    val publishedAt: LocalDateTime?,
    val retrievedAt: LocalDateTime,
    val quote: String?,
    val sourceQuality: String,
    val confidence: Double?
)

data class BillAction(
    val id: UUID,
    val billId: UUID,
    val actionDate: LocalDate,
    val actionText: String,
    val sourceCitationId: UUID?
)
