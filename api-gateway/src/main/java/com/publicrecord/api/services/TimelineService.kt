package com.publicrecord.api.services

import com.publicrecord.api.dto.TimelineAggregateDto
import com.publicrecord.api.dto.TimelineItemDto
import com.publicrecord.api.dto.TimelineStatsDto
import com.publicrecord.common.privacy.PrivacySafetyService
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.ClaimRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.OfficeElectionRepository
import com.publicrecord.storage.repositories.PublicStatementRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import com.publicrecord.storage.repositories.VotingRecordRepository
import java.time.LocalDateTime
import java.util.UUID

class TimelineService(
    private val votingRecordRepository: VotingRecordRepository,
    private val billRepository: BillRepository,
    private val contentItemRepository: ContentItemRepository,
    private val publicStatementRepository: PublicStatementRepository,
    private val claimRepository: ClaimRepository,
    private val sourceCitationRepository: SourceCitationRepository,
    private val officeElectionRepository: OfficeElectionRepository
) {
    fun aggregate(politicianId: UUID, category: String?, limit: Int): TimelineAggregateDto {
        val boundedLimit = limit.coerceIn(1, 250)
        val requestedCategories = category
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
        val voted = votingRecordRepository.findJoinedByPoliticianId(politicianId, 250)
        val sponsored = billRepository.findByIntroducedBy(politicianId, 250)
        val content = contentItemRepository.findByPoliticianId(politicianId, 250, 0)
        val statements = publicStatementRepository.findByPoliticianId(politicianId, 250)
        val claims = claimRepository.findByPoliticianId(politicianId, null, null, 250)
        val offices = officeElectionRepository.findOfficeHistory(politicianId)
        val elections = officeElectionRepository.findElectionHistoryForPolitician(politicianId)

        val voteItems = voted.map { vote ->
            TimelineItemDto(
                id = "vote:${vote.id}",
                date = vote.voteDate.atStartOfDay(),
                category = "Vote",
                title = "${vote.voteType} on ${vote.billNumber ?: "bill"}",
                description = vote.billTitle,
                sourceUrl = vote.billUrl,
                sourceName = "Voting record",
                targetType = "VOTE",
                targetId = vote.id,
                evidenceType = "VOTING_RECORD",
                publishable = true
            )
        }

        val sponsoredItems = sponsored.map { bill ->
            TimelineItemDto(
                id = "bill:${bill.id}",
                date = bill.introducedDate.atStartOfDay(),
                category = "Bill",
                title = "Sponsored ${bill.billNumber}",
                description = bill.title,
                sourceUrl = bill.billUrl,
                sourceName = "Official bill record",
                targetType = "BILL",
                targetId = bill.id,
                evidenceType = "OFFICIAL_RECORD",
                publishable = true
            )
        }

        val actionItems = sponsored.flatMap { bill ->
            billRepository.findActions(bill.id, 25).map { action ->
                TimelineItemDto(
                    id = "bill-action:${action.id}",
                    date = action.actionDate.atStartOfDay(),
                    category = "Bill Action",
                    title = "${bill.billNumber} action",
                    description = action.actionText,
                    sourceUrl = bill.billUrl,
                    sourceName = "Official bill action",
                    targetType = "BILL_ACTION",
                    targetId = action.id,
                    evidenceType = "OFFICIAL_RECORD",
                    publishable = true
                )
            }
        }

        val statementItems = statements.map { statement ->
            val suspicious = suspiciousText(statement.title) || suspiciousText(statement.body) || suspiciousText(statement.quote)
            val privacy = PrivacySafetyService.evaluate(statement.quote ?: statement.body)
            TimelineItemDto(
                id = "statement:${statement.id}",
                date = statement.statementDate,
                category = "Statement",
                title = statement.title,
                description = privacy.redactedText.ifBlank { null },
                sourceUrl = statement.sourceCitationId?.let {
                    sourceCitationRepository.findByTarget("STATEMENT", statement.id, 1).firstOrNull()?.url
                },
                sourceName = statement.venue,
                targetType = "STATEMENT",
                targetId = statement.id,
                evidenceType = statement.statementType,
                publishable = !suspicious && privacy.safeForPublicDisplay,
                warnings = buildList {
                    if (suspicious) add("Statement text needs review before public AI processing.")
                    addAll(privacy.warnings)
                }
            )
        }

        val claimItems = claims.map { claim ->
            val warnings = mutableListOf<String>()
            if (claim.citationCount <= 0) warnings += "Claim has no citation support."
            if (claim.claimType.isBlank()) warnings += "Claim category is missing."
            TimelineItemDto(
                id = "claim:${claim.id}",
                date = claim.firstSeenAt ?: claim.createdAt,
                category = "Claim",
                title = claim.claimType.replace("_", " "),
                description = claim.claimText,
                sourceUrl = sourceCitationRepository.findByTarget("CLAIM", claim.id, 1).firstOrNull()?.url,
                sourceName = "Claim review",
                targetType = "CLAIM",
                targetId = claim.id,
                evidenceType = claim.claimType,
                publishable = warnings.isEmpty(),
                warnings = warnings
            )
        }

        val contentItems = content.map { item ->
            val suspicious = suspiciousText(item.title) || suspiciousText(item.textBody)
            val privacy = PrivacySafetyService.evaluate(item.textBody)
            TimelineItemDto(
                id = "content:${item.id}",
                date = item.publishedAt,
                category = item.contentType.replaceFirstChar { it.uppercase() },
                title = item.title,
                description = privacy.redactedText.ifBlank { null },
                sourceUrl = item.sourceUrl,
                sourceName = item.provenance?.sourceType,
                targetType = "CONTENT_ITEM",
                targetId = item.id,
                evidenceType = item.contentType.uppercase(),
                publishable = !suspicious && privacy.safeForPublicDisplay,
                warnings = buildList {
                    if (suspicious) add("Content text needs review before public AI processing.")
                    addAll(privacy.warnings)
                }
            )
        }

        val officeItems = offices.map { office ->
            TimelineItemDto(
                id = "office:${office.officeId}:${office.startDate}",
                date = office.startDate.atStartOfDay(),
                category = "Office",
                title = if (office.isCurrent) "Started current office" else "Started office",
                description = "${office.title} · ${office.jurisdiction}",
                sourceUrl = office.sourceUrl,
                sourceName = "Office source",
                targetType = "OFFICE",
                targetId = office.officeId,
                evidenceType = "OFFICE_RECORD",
                publishable = true
            )
        }

        val electionItems = elections.map { election ->
            TimelineItemDto(
                id = "election:${election.electionId}",
                date = election.electionDate.atStartOfDay(),
                category = "Election",
                title = "${election.electionType} election",
                description = "${election.jurisdiction} · ${election.cycleYear}",
                sourceUrl = election.sourceUrl,
                sourceName = "Election source",
                targetType = "ELECTION",
                targetId = election.electionId,
                evidenceType = "ELECTION_RECORD",
                publishable = true
            )
        }

        val allItems = (voteItems + sponsoredItems + actionItems + statementItems + claimItems + contentItems + officeItems + electionItems)
            .distinctBy { "${it.targetType}:${it.targetId}:${it.category}:${it.date}" }
            .sortedByDescending { it.date }

        val filtered = allItems
            .filter { requestedCategories.isEmpty() || requestedCategories.any { requested -> it.category.equals(requested, ignoreCase = true) } }
            .sortedByDescending { it.date }
            .take(boundedLimit)

        return TimelineAggregateDto(
            politicianId = politicianId,
            generatedAt = LocalDateTime.now(),
            stats = TimelineStatsDto(
                total = allItems.size,
                byCategory = allItems.groupingBy { it.category }.eachCount().toSortedMap(),
                publishableCount = allItems.count { it.publishable },
                reviewRequiredCount = allItems.count { !it.publishable },
                latestActivityAt = filtered.firstOrNull()?.date
            ),
            items = filtered
        )
    }

    private fun suspiciousText(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val lowered = value.lowercase()
        return listOf("ignore previous instructions", "system prompt", "developer message", "jailbreak").any { lowered.contains(it) }
    }
}
