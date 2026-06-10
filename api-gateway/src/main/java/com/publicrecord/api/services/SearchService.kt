package com.publicrecord.api.services

import com.publicrecord.api.dto.SearchGroup
import com.publicrecord.api.dto.SearchResponse
import com.publicrecord.api.dto.SearchResult
import com.publicrecord.common.privacy.PrivacySafetyService
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.ClaimRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.PoliticianRepository
import com.publicrecord.storage.repositories.PublicStatementRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import com.publicrecord.storage.repositories.VotingRecordRepository

class SearchService(
    private val politicianRepository: PoliticianRepository,
    private val billRepository: BillRepository,
    private val claimRepository: ClaimRepository,
    private val contentItemRepository: ContentItemRepository,
    private val publicStatementRepository: PublicStatementRepository,
    private val sourceCitationRepository: SourceCitationRepository,
    private val votingRecordRepository: VotingRecordRepository
) {
    fun search(query: String, limit: Int = 10): SearchResponse {
        val boundedLimit = limit.coerceIn(1, 25)
        val politicians = politicianRepository.searchByName(query, boundedLimit).map {
            val privacy = PrivacySafetyService.evaluate(it.biography)
            SearchResult(
                id = it.id.toString(),
                title = "${it.firstName} ${it.lastName}",
                subtitle = listOf(it.office, it.state, it.party).filter { value -> value.isNotBlank() }.joinToString(" · "),
                description = privacy.redactedText.ifBlank { null },
                url = "/politicians/${it.id}",
                reviewWarnings = privacy.warnings
            )
        }
        val bills = billRepository.searchWithSponsor(query, null, boundedLimit).map {
            val privacy = PrivacySafetyService.evaluate(it.description)
            SearchResult(
                id = it.id.toString(),
                title = "${it.billNumber}: ${it.title}",
                subtitle = it.status,
                description = privacy.redactedText.ifBlank { null },
                url = "/bills/${it.id}",
                date = it.lastActionDate?.toString() ?: it.introducedDate.toString(),
                reviewWarnings = privacy.warnings
            )
        }
        val claims = claimRepository.search(query, null, null, boundedLimit)
            .filter { it.citationCount > 0 || it.status == "VERIFIED" }
            .map {
                val warnings = buildList {
                    if (it.status == "UNRESOLVED") add("Unresolved claim; show citation context before public display.")
                    if (it.citationCount == 0) add("No citation linked yet.")
                }
                SearchResult(
                    id = it.id.toString(),
                    title = it.claimText.take(120),
                    subtitle = "${it.claimType.replace('_', ' ')} · ${it.status}",
                    trustContext = "${it.citationCount} citation${if (it.citationCount == 1) "" else "s"}",
                    reviewWarnings = warnings
                )
            }
        val statements = publicStatementRepository.search(query, null, boundedLimit).map {
            val text = it.quote ?: it.body
            val suspicious = containsPromptInjectionLikeText("${it.title} ${it.body.orEmpty()} ${it.quote.orEmpty()}")
            val privacy = PrivacySafetyService.evaluate(text)
            SearchResult(
                id = it.id.toString(),
                title = it.title,
                subtitle = it.statementType.replace('_', ' '),
                description = privacy.redactedText.ifBlank { null },
                date = it.statementDate.toString(),
                reviewWarnings = buildList {
                    if (suspicious) add("Statement text needs review before AI summarization.")
                    addAll(privacy.warnings)
                }
            )
        }
        val media = contentItemRepository.searchByKeyword(query, boundedLimit).map {
            val suspicious = containsPromptInjectionLikeText("${it.title} ${it.textBody.orEmpty()}")
            val privacy = PrivacySafetyService.evaluate(it.textBody)
            SearchResult(
                id = it.id.toString(),
                title = it.title,
                subtitle = it.contentType,
                description = privacy.redactedText.ifBlank { null },
                url = it.sourceUrl,
                date = it.publishedAt.toString(),
                reviewWarnings = buildList {
                    if (suspicious) add("Media text needs review before AI summarization.")
                    addAll(privacy.warnings)
                }
            )
        }
        val citations = sourceCitationRepository.search(null, null, query, boundedLimit).map {
            SearchResult(
                id = it.id.toString(),
                title = it.title ?: it.url,
                subtitle = "${it.citationType.replace('_', ' ')} · ${it.sourceQuality.replace('_', ' ')}",
                url = it.url,
                source = it.sourceName,
                date = it.publishedAt?.toString() ?: it.retrievedAt.toString(),
                trustContext = "Source quality: ${it.sourceQuality}"
            )
        }
        val votes = votingRecordRepository.searchJoined(query, boundedLimit).map {
            SearchResult(
                id = it.id.toString(),
                title = "${it.voteType} on ${it.billNumber ?: "bill"}",
                subtitle = listOfNotNull(it.politicianName, it.billTitle).joinToString(" · "),
                url = it.billUrl,
                date = it.voteDate.toString(),
                trustContext = "Official voting record"
            )
        }

        val groups = listOf(
            SearchGroup("politicians", "Politicians", politicians),
            SearchGroup("bills", "Bills", bills),
            SearchGroup("votes", "Votes", votes),
            SearchGroup("statements", "Statements", statements),
            SearchGroup("claims", "Claims With Evidence", claims),
            SearchGroup("media", "Articles And Media", media),
            SearchGroup("citations", "Source Citations", citations)
        )
        return SearchResponse(query, groups.sumOf { it.results.size }, groups)
    }

    private fun containsPromptInjectionLikeText(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("ignore previous instructions", "system prompt", "developer message", "jailbreak", "do not follow").any {
            normalized.contains(it)
        }
    }
}
