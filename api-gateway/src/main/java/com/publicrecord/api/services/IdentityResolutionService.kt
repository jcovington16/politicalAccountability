package com.publicrecord.api.services

import com.publicrecord.common.models.IdentityMatchCandidate
import com.publicrecord.common.models.IdentityResolutionResult
import com.publicrecord.storage.repositories.ExternalIdentifierRepository
import com.publicrecord.storage.repositories.PoliticianRepository
import java.math.BigDecimal

class IdentityResolutionService(
    private val politicianRepository: PoliticianRepository,
    private val externalIdentifierRepository: ExternalIdentifierRepository
) {
    fun resolvePolitician(
        query: String?,
        sourceSystem: String?,
        externalId: String?,
        state: String?,
        party: String?,
        office: String?,
        limit: Int = 10
    ): IdentityResolutionResult {
        val exactIdentifier = if (!sourceSystem.isNullOrBlank() && !externalId.isNullOrBlank()) {
            externalIdentifierRepository.findBySource("POLITICIAN", sourceSystem, externalId)
        } else {
            null
        }

        if (exactIdentifier != null) {
            val politician = politicianRepository.findById(exactIdentifier.entityId)
            if (politician != null) {
                return IdentityResolutionResult(
                    query = query,
                    sourceSystem = sourceSystem,
                    externalId = externalId,
                    candidates = listOf(
                        IdentityMatchCandidate(
                            politician = politician,
                            confidence = BigDecimal("100.00"),
                            reasons = listOf("Exact external identifier match: $sourceSystem/$externalId"),
                            externalIdentifiers = externalIdentifierRepository.findByEntity("POLITICIAN", politician.id),
                            socialAccounts = externalIdentifierRepository.findSocialAccountsByPolitician(politician.id)
                        )
                    ),
                    needsReview = false,
                    reviewReason = null
                )
            }
        }

        val candidates = if (query.isNullOrBlank()) {
            emptyList()
        } else {
            politicianRepository.searchByName(query, limit.coerceIn(1, 25))
                .map { politician ->
                    val scored = scoreCandidate(query, state, party, office, politician)
                    IdentityMatchCandidate(
                        politician = politician,
                        confidence = scored.first,
                        reasons = scored.second,
                        externalIdentifiers = externalIdentifierRepository.findByEntity("POLITICIAN", politician.id),
                        socialAccounts = externalIdentifierRepository.findSocialAccountsByPolitician(politician.id)
                    )
                }
                .sortedByDescending { it.confidence }
        }

        val top = candidates.firstOrNull()
        val second = candidates.drop(1).firstOrNull()
        val needsReview = top == null ||
            top.confidence < BigDecimal("85.00") ||
            (second != null && top.confidence.subtract(second.confidence) < BigDecimal("15.00"))

        return IdentityResolutionResult(
            query = query,
            sourceSystem = sourceSystem,
            externalId = externalId,
            candidates = candidates,
            needsReview = needsReview,
            reviewReason = when {
                top == null -> "No candidate matched the supplied identity hints."
                top.confidence < BigDecimal("85.00") -> "Best candidate confidence is below the auto-link threshold."
                second != null && top.confidence.subtract(second.confidence) < BigDecimal("15.00") ->
                    "Top candidates are too close together; manual review should confirm the match."
                else -> null
            }
        )
    }

    private fun scoreCandidate(
        query: String,
        state: String?,
        party: String?,
        office: String?,
        politician: com.publicrecord.common.models.Politician
    ): Pair<BigDecimal, List<String>> {
        val reasons = mutableListOf<String>()
        var score = 0
        val normalizedQuery = normalize(query)
        val fullName = normalize("${politician.firstName} ${politician.lastName}")

        if (fullName == normalizedQuery) {
            score += 55
            reasons += "Full name matches exactly."
        } else if (fullName.contains(normalizedQuery) || normalizedQuery.contains(fullName)) {
            score += 40
            reasons += "Full name contains the query."
        } else if (normalize(politician.lastName) == normalizedQuery.substringAfterLast(" ")) {
            score += 25
            reasons += "Last name matches."
        }

        if (!state.isNullOrBlank() && politician.state.equals(state, ignoreCase = true)) {
            score += 20
            reasons += "State matches."
        }
        if (!party.isNullOrBlank() && politician.party.equals(party, ignoreCase = true)) {
            score += 10
            reasons += "Party matches."
        }
        if (!office.isNullOrBlank() && normalize(politician.office).contains(normalize(office))) {
            score += 10
            reasons += "Office context matches."
        }

        if (reasons.isEmpty()) reasons += "Weak name-only match."
        return BigDecimal(score.coerceIn(0, 100)).setScale(2) to reasons
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
}
