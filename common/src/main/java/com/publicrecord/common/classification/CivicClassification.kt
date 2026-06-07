package com.publicrecord.common.classification

enum class SentimentLabel {
    POSITIVE,
    NEGATIVE,
    MIXED,
    NEUTRAL,
    UNKNOWN
}

enum class CivicImpactLabel {
    PROBLEM_SOLVING,
    PROBLEMATIC,
    HARMFUL_RISK,
    PUBLIC_SERVICE,
    ACCOUNTABILITY_CONCERN,
    INFORMATIONAL,
    UNKNOWN
}

enum class HarmRiskLevel {
    NONE,
    LOW,
    MEDIUM,
    HIGH
}

enum class ReviewStatus {
    MACHINE_CLASSIFIED,
    NEEDS_REVIEW,
    HUMAN_REVIEWED,
    DISPUTED,
    REJECTED
}

enum class ClassificationConfidence {
    HIGH,
    MEDIUM,
    LOW
}

data class ClassificationInput(
    val title: String? = null,
    val text: String,
    val sourceQuality: String? = null,
    val citationCount: Int = 0,
    val isDirectQuote: Boolean = false,
    val isOfficialRecord: Boolean = false,
    val reviewerStatus: ReviewStatus? = null
)

data class CivicClassification(
    val sentiment: SentimentLabel,
    val impact: CivicImpactLabel,
    val harmRisk: HarmRiskLevel,
    val problemSolving: Boolean,
    val problematic: Boolean,
    val reviewStatus: ReviewStatus,
    val confidence: ClassificationConfidence,
    val labels: List<String>,
    val explanation: String,
    val reviewWarnings: List<String>
)

object CivicClassificationService {
    private val harmfulTerms = listOf(
        "violence",
        "violent",
        "threat",
        "harass",
        "hate",
        "dehumanize",
        "racial slur",
        "kill",
        "attack"
    )
    private val problematicTerms = listOf(
        "corruption",
        "bribery",
        "fraud",
        "ethics",
        "investigation",
        "misconduct",
        "lawsuit",
        "indictment",
        "false claim",
        "misleading"
    )
    private val problemSolvingTerms = listOf(
        "introduced",
        "passed",
        "funding",
        "improve",
        "reduce",
        "expand access",
        "constituent services",
        "bipartisan",
        "oversight",
        "protect",
        "lower costs"
    )
    private val publicServiceTerms = listOf(
        "disaster relief",
        "veterans",
        "infrastructure",
        "healthcare",
        "education",
        "public safety",
        "housing",
        "jobs"
    )
    private val positiveTerms = listOf("improve", "protect", "support", "passed", "funding", "relief", "access", "bipartisan")
    private val negativeTerms = listOf("fraud", "misconduct", "threat", "violence", "harm", "failed", "corruption", "misleading")

    fun classify(input: ClassificationInput): CivicClassification {
        val combined = "${input.title.orEmpty()} ${input.text}".lowercase()
        val harmfulHits = harmfulTerms.filter { combined.contains(it) }
        val problematicHits = problematicTerms.filter { combined.contains(it) }
        val solvingHits = problemSolvingTerms.filter { combined.contains(it) }
        val serviceHits = publicServiceTerms.filter { combined.contains(it) }
        val positiveHits = positiveTerms.count { combined.contains(it) }
        val negativeHits = negativeTerms.count { combined.contains(it) }

        val harmRisk = when {
            harmfulHits.size >= 2 -> HarmRiskLevel.HIGH
            harmfulHits.isNotEmpty() -> HarmRiskLevel.MEDIUM
            problematicHits.isNotEmpty() -> HarmRiskLevel.LOW
            else -> HarmRiskLevel.NONE
        }

        val impact = when {
            harmRisk == HarmRiskLevel.HIGH || harmRisk == HarmRiskLevel.MEDIUM -> CivicImpactLabel.HARMFUL_RISK
            problematicHits.isNotEmpty() -> CivicImpactLabel.PROBLEMATIC
            solvingHits.isNotEmpty() -> CivicImpactLabel.PROBLEM_SOLVING
            serviceHits.isNotEmpty() -> CivicImpactLabel.PUBLIC_SERVICE
            input.isOfficialRecord -> CivicImpactLabel.INFORMATIONAL
            else -> CivicImpactLabel.UNKNOWN
        }

        val sentiment = when {
            positiveHits > 0 && negativeHits > 0 -> SentimentLabel.MIXED
            positiveHits > negativeHits -> SentimentLabel.POSITIVE
            negativeHits > positiveHits -> SentimentLabel.NEGATIVE
            input.isOfficialRecord -> SentimentLabel.NEUTRAL
            else -> SentimentLabel.UNKNOWN
        }

        val warnings = mutableListOf<String>()
        if (input.citationCount <= 0 && !input.isOfficialRecord) warnings += "Classification needs at least one citation before public display."
        if (harmRisk >= HarmRiskLevel.MEDIUM) warnings += "Potentially harmful language requires human review."
        if (impact == CivicImpactLabel.PROBLEMATIC && input.citationCount < 2) warnings += "Problematic conduct labels should have multiple citations."
        if (input.sourceQuality == "SOCIAL_MEDIA" && !input.isDirectQuote) warnings += "Social posts should be treated as direct statements, not verified facts."
        if (containsPromptInjection(combined)) warnings += "Text contains prompt-injection-like instructions and needs review."

        val reviewStatus = input.reviewerStatus ?: if (warnings.isEmpty()) ReviewStatus.MACHINE_CLASSIFIED else ReviewStatus.NEEDS_REVIEW
        val confidence = when {
            reviewStatus == ReviewStatus.HUMAN_REVIEWED -> ClassificationConfidence.HIGH
            warnings.isNotEmpty() -> ClassificationConfidence.LOW
            input.isOfficialRecord || input.citationCount >= 2 -> ClassificationConfidence.MEDIUM
            else -> ClassificationConfidence.LOW
        }

        val labels = buildList {
            add("sentiment:${sentiment.name}")
            add("impact:${impact.name}")
            add("harm:${harmRisk.name}")
            if (solvingHits.isNotEmpty()) add("problem_solving")
            if (problematicHits.isNotEmpty()) add("problematic")
            if (serviceHits.isNotEmpty()) add("public_service")
        }

        return CivicClassification(
            sentiment = sentiment,
            impact = impact,
            harmRisk = harmRisk,
            problemSolving = solvingHits.isNotEmpty(),
            problematic = problematicHits.isNotEmpty() || harmRisk != HarmRiskLevel.NONE,
            reviewStatus = reviewStatus,
            confidence = confidence,
            labels = labels,
            explanation = buildExplanation(sentiment, impact, harmRisk, solvingHits, problematicHits, serviceHits),
            reviewWarnings = warnings
        )
    }

    private fun containsPromptInjection(value: String): Boolean {
        return listOf("ignore previous instructions", "system prompt", "developer message", "jailbreak")
            .any { value.contains(it) }
    }

    private fun buildExplanation(
        sentiment: SentimentLabel,
        impact: CivicImpactLabel,
        harmRisk: HarmRiskLevel,
        solvingHits: List<String>,
        problematicHits: List<String>,
        serviceHits: List<String>
    ): String {
        val reasons = mutableListOf<String>()
        if (solvingHits.isNotEmpty()) reasons += "problem-solving terms: ${solvingHits.take(3).joinToString(", ")}"
        if (problematicHits.isNotEmpty()) reasons += "problematic terms: ${problematicHits.take(3).joinToString(", ")}"
        if (serviceHits.isNotEmpty()) reasons += "public-service topics: ${serviceHits.take(3).joinToString(", ")}"
        if (reasons.isEmpty()) reasons += "no strong civic-impact keywords were found"
        return "Classified as $impact with $sentiment sentiment and $harmRisk harm risk because ${reasons.joinToString("; ")}."
    }
}
