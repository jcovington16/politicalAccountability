package com.publicrecord.api.services

import com.publicrecord.api.dto.SearchGroup
import com.publicrecord.api.dto.SearchResponse
import com.publicrecord.api.dto.SearchResult
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
            SearchResult(
                id = it.id.toString(),
                title = "${it.firstName} ${it.lastName}",
                subtitle = listOf(it.office, it.state, it.party).filter { value -> value.isNotBlank() }.joinToString(" · "),
                description = it.biography,
                url = "/politicians/${it.id}"
            )
        }
        val bills = billRepository.searchWithSponsor(query, null, boundedLimit).map {
            SearchResult(
                id = it.id.toString(),
                title = "${it.billNumber}: ${it.title}",
                subtitle = it.status,
                description = it.description,
                url = "/bills/${it.id}",
                date = it.lastActionDate?.toString() ?: it.introducedDate.toString()
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
            val suspicious = containsPromptInjectionLikeText("${it.title} ${it.body.orEmpty()} ${it.quote.orEmpty()}")
            SearchResult(
                id = it.id.toString(),
                title = it.title,
                subtitle = it.statementType.replace('_', ' '),
                description = it.quote ?: it.body,
                date = it.statementDate.toString(),
                reviewWarnings = if (suspicious) listOf("Statement text needs review before AI summarization.") else emptyList()
            )
        }
        val media = contentItemRepository.searchByKeyword(query, boundedLimit).map {
            val suspicious = containsPromptInjectionLikeText("${it.title} ${it.textBody.orEmpty()}")
            SearchResult(
                id = it.id.toString(),
                title = it.title,
                subtitle = it.contentType,
                description = it.textBody,
                url = it.sourceUrl,
                date = it.publishedAt.toString(),
                reviewWarnings = if (suspicious) listOf("Media text needs review before AI summarization.") else emptyList()
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
