package com.publicrecord.api.services

import com.publicrecord.api.dto.ReviewQueueItem
import com.publicrecord.api.dto.ReviewQueueResponse
import com.publicrecord.storage.repositories.ClaimRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.PublicStatementRepository
import java.time.LocalDateTime

class ReviewQueueService(
    private val claimRepository: ClaimRepository,
    private val contentItemRepository: ContentItemRepository,
    private val publicStatementRepository: PublicStatementRepository
) {
    fun queue(limit: Int = 50): ReviewQueueResponse {
        val boundedLimit = limit.coerceIn(1, 100)
        val claims = claimRepository.search(null, null, null, boundedLimit)
            .filter { it.status == "UNRESOLVED" || it.citationCount == 0 || it.claimType == "ALLEGATION" }
            .map {
                ReviewQueueItem(
                    targetType = "CLAIM",
                    targetId = it.id.toString(),
                    title = it.claimText.take(160),
                    reason = when {
                        it.citationCount == 0 -> "Claim has no linked citations."
                        it.claimType == "ALLEGATION" -> "Allegation requires citation and reviewer confirmation."
                        else -> "Claim is unresolved."
                    },
                    severity = if (it.citationCount == 0 || it.claimType == "ALLEGATION") "HIGH" else "MEDIUM",
                    createdAt = it.createdAt.toString()
                )
            }
        val statements = publicStatementRepository.search(null, null, boundedLimit)
            .filter { containsPromptInjectionLikeText("${it.title} ${it.body.orEmpty()} ${it.quote.orEmpty()}") || it.sourceCitationId == null }
            .map {
                ReviewQueueItem(
                    targetType = "STATEMENT",
                    targetId = it.id.toString(),
                    title = it.title,
                    reason = if (it.sourceCitationId == null) "Statement has no source citation." else "Statement text contains prompt-injection-like instructions.",
                    severity = if (it.sourceCitationId == null) "MEDIUM" else "HIGH",
                    createdAt = it.statementDate.toString()
                )
            }
        val media = contentItemRepository.searchByKeyword("", boundedLimit)
            .filter { containsPromptInjectionLikeText("${it.title} ${it.textBody.orEmpty()}") || it.contentType in setOf("article", "video", "social_post") }
            .take(boundedLimit)
            .map {
                ReviewQueueItem(
                    targetType = "CONTENT_ITEM",
                    targetId = it.id.toString(),
                    title = it.title,
                    source = it.contentType,
                    reason = "Media is a citation candidate and should be reviewed before it becomes a claim or direct quote.",
                    severity = if (containsPromptInjectionLikeText("${it.title} ${it.textBody.orEmpty()}")) "HIGH" else "LOW",
                    sourceUrl = it.sourceUrl,
                    createdAt = it.publishedAt.toString()
                )
            }
        val items = (claims + statements + media)
            .sortedByDescending { it.createdAt ?: "" }
            .take(boundedLimit)
        return ReviewQueueResponse(LocalDateTime.now().toString(), items.size, items)
    }

    private fun containsPromptInjectionLikeText(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("ignore previous instructions", "system prompt", "developer message", "jailbreak", "do not follow").any {
            normalized.contains(it)
        }
    }
}
