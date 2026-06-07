package com.publicrecord.common.models

import java.time.LocalDateTime
import java.util.UUID

data class Claim(
    val id: UUID,
    val politicianId: UUID?,
    val statementId: UUID?,
    val claimText: String,
    val claimType: String,
    val status: String,
    val confidence: Double?,
    val firstSeenAt: LocalDateTime?,
    val lastReviewedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val citationCount: Int = 0,
    val latestCitationAt: LocalDateTime? = null
)

data class FactCheck(
    val id: UUID,
    val claimId: UUID,
    val rating: String,
    val summary: String,
    val checkedBy: String?,
    val checkedAt: LocalDateTime,
    val sourceCitationId: UUID?
)

data class SourceRegistryEntry(
    val id: UUID,
    val name: String,
    val sourceType: String,
    val homepageUrl: String?,
    val owningEntity: String?,
    val reputationScore: Double?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
