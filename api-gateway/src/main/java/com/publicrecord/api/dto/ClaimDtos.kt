package com.publicrecord.api.dto

import com.publicrecord.common.models.Claim
import com.publicrecord.common.models.FactCheck
import com.publicrecord.common.models.SourceCitation
import com.publicrecord.common.models.SourceRegistryEntry
import com.publicrecord.common.trust.InformationType
import com.publicrecord.common.trust.SourceQuality
import com.publicrecord.common.trust.TrustScore
import com.publicrecord.common.trust.TrustScoreInput
import com.publicrecord.common.trust.TrustScoringService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class ClaimDto(
    val id: UUID,
    val politicianId: UUID?,
    val statementId: UUID?,
    val claimText: String,
    val claimType: String,
    val status: String,
    val confidence: Double?,
    val firstSeenAt: LocalDateTime?,
    val lastReviewedAt: LocalDateTime?,
    val citationCount: Int,
    val trust: TrustScore,
    val publishable: Boolean,
    val reviewWarnings: List<String>,
    val factChecks: List<FactCheckDto>
)

data class FactCheckDto(
    val id: UUID,
    val claimId: UUID,
    val rating: String,
    val summary: String,
    val checkedBy: String?,
    val checkedAt: LocalDateTime,
    val sourceCitationId: UUID?
)

data class CitationDto(
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
    val confidence: Double?,
    val manipulationWarnings: List<String>
)

data class SourceRegistryDto(
    val id: UUID,
    val name: String,
    val sourceType: String,
    val homepageUrl: String?,
    val owningEntity: String?,
    val reputationScore: Double?,
    val notes: String?,
    val manipulationWarnings: List<String>
)

fun Claim.toDto(factChecks: List<FactCheck> = emptyList()): ClaimDto {
    val warnings = claimReviewWarnings(this)
    return ClaimDto(
        id = id,
        politicianId = politicianId,
        statementId = statementId,
        claimText = claimText,
        claimType = claimType,
        status = status,
        confidence = confidence,
        firstSeenAt = firstSeenAt,
        lastReviewedAt = lastReviewedAt,
        citationCount = citationCount,
        trust = TrustScoringService.score(
            TrustScoreInput(
                informationType = runCatching { InformationType.valueOf(claimType) }.getOrDefault(InformationType.UNRESOLVED_CLAIM),
                sourceQuality = SourceQuality.UNKNOWN,
                citationCount = citationCount,
                publishedDate = firstSeenAt?.toLocalDate() ?: createdAt.toLocalDate(),
                asOfDate = LocalDate.now()
            )
        ),
        publishable = warnings.isEmpty(),
        reviewWarnings = warnings,
        factChecks = factChecks.map { it.toDto() }
    )
}

fun FactCheck.toDto(): FactCheckDto {
    return FactCheckDto(
        id = id,
        claimId = claimId,
        rating = rating,
        summary = summary,
        checkedBy = checkedBy,
        checkedAt = checkedAt,
        sourceCitationId = sourceCitationId
    )
}

fun SourceCitation.toDto(): CitationDto {
    return CitationDto(
        id = id,
        sourceName = sourceName,
        sourceType = sourceType,
        citationType = citationType,
        targetId = targetId,
        title = title,
        url = url,
        archiveUrl = archiveUrl,
        publishedAt = publishedAt,
        retrievedAt = retrievedAt,
        quote = quote,
        sourceQuality = sourceQuality,
        confidence = confidence,
        manipulationWarnings = sourceManipulationWarnings(sourceName, sourceType, url, sourceQuality, confidence)
    )
}

fun SourceRegistryEntry.toDto(): SourceRegistryDto {
    return SourceRegistryDto(
        id = id,
        name = name,
        sourceType = sourceType,
        homepageUrl = homepageUrl,
        owningEntity = owningEntity,
        reputationScore = reputationScore,
        notes = notes,
        manipulationWarnings = sourceManipulationWarnings(name, sourceType, homepageUrl, sourceType, reputationScore)
    )
}

private fun claimReviewWarnings(claim: Claim): List<String> {
    val warnings = mutableListOf<String>()
    if (claim.claimType.isBlank()) warnings += "Claim category is required before publishing."
    if (claim.citationCount <= 0) warnings += "At least one citation is required before publishing."
    if (claim.claimType in setOf("ALLEGATION", "UNRESOLVED_CLAIM") && claim.status == "VERIFIED") {
        warnings += "Allegations and unresolved claims cannot be marked verified without reclassification."
    }
    return warnings
}

private fun sourceManipulationWarnings(
    sourceName: String?,
    sourceType: String?,
    url: String?,
    qualityOrType: String?,
    confidenceOrReputation: Double?
): List<String> {
    val warnings = mutableListOf<String>()
    if (url != null && runCatching { java.net.URI.create(url).scheme }.getOrNull() != "https") {
        warnings += "Source URL is not HTTPS."
    }
    if (qualityOrType == "UNKNOWN" || sourceType == "UNKNOWN") {
        warnings += "Source quality is unknown."
    }
    if ((confidenceOrReputation ?: 1.0) < 50.0 && (confidenceOrReputation ?: 1.0) > 1.0) {
        warnings += "Source reputation is low."
    }
    if (sourceName.isNullOrBlank()) {
        warnings += "Source name is missing."
    }
    return warnings
}
