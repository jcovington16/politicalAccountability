package com.publicrecord.common.trust

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

enum class InformationType {
    VERIFIED_FACT,
    DIRECT_QUOTE,
    VOTING_RECORD,
    ALLEGATION,
    OPINION_PIECE,
    UNRESOLVED_CLAIM
}

enum class SourceQuality(val weight: Double) {
    OFFICIAL_RECORD(1.0),
    PRIMARY_SOURCE(0.9),
    REPUTABLE_NEWS(0.75),
    ADVOCACY_OR_PARTISAN(0.45),
    SOCIAL_MEDIA(0.4),
    UNKNOWN(0.2)
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

data class TrustScore(
    val informationType: InformationType,
    val sourceQuality: SourceQuality,
    val citationCount: Int,
    val recencyDays: Long?,
    val confidenceLevel: ConfidenceLevel,
    val score: Double,
    val explanation: String
)

data class TrustScoreInput(
    val informationType: InformationType,
    val sourceQuality: SourceQuality,
    val citationCount: Int = 0,
    val publishedDate: LocalDate? = null,
    val asOfDate: LocalDate = LocalDate.now()
)

object TrustScoringService {
    fun score(input: TrustScoreInput): TrustScore {
        val typeWeight = when (input.informationType) {
            InformationType.VERIFIED_FACT -> 0.95
            InformationType.DIRECT_QUOTE -> 0.9
            InformationType.VOTING_RECORD -> 1.0
            InformationType.ALLEGATION -> 0.45
            InformationType.OPINION_PIECE -> 0.3
            InformationType.UNRESOLVED_CLAIM -> 0.25
        }

        val citationWeight = min(1.0, max(0, input.citationCount) / 5.0)
        val recencyDays = input.publishedDate?.let { ChronoUnit.DAYS.between(it, input.asOfDate) }
        val recencyWeight = when {
            recencyDays == null -> 0.55
            recencyDays <= 30 -> 1.0
            recencyDays <= 180 -> 0.85
            recencyDays <= 730 -> 0.65
            else -> 0.45
        }

        val score = (
            typeWeight * 0.35 +
                input.sourceQuality.weight * 0.30 +
                citationWeight * 0.20 +
                recencyWeight * 0.15
            ).coerceIn(0.0, 1.0)

        val confidence = when {
            score >= 0.80 -> ConfidenceLevel.HIGH
            score >= 0.55 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        return TrustScore(
            informationType = input.informationType,
            sourceQuality = input.sourceQuality,
            citationCount = max(0, input.citationCount),
            recencyDays = recencyDays,
            confidenceLevel = confidence,
            score = "%.2f".format(score).toDouble(),
            explanation = explanation(input.informationType, input.sourceQuality, confidence)
        )
    }

    private fun explanation(
        informationType: InformationType,
        sourceQuality: SourceQuality,
        confidenceLevel: ConfidenceLevel
    ): String {
        return "Classified as $informationType from $sourceQuality with $confidenceLevel confidence."
    }
}
