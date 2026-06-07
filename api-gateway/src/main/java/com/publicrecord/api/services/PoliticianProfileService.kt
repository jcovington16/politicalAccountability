package com.publicrecord.api.services

import com.publicrecord.api.dto.PoliticianProfileDto
import com.publicrecord.api.dto.ProfileBillVoteDto
import com.publicrecord.api.dto.ProfileTimelineItemDto
import com.publicrecord.api.dto.ProfileTrustSummaryDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.common.trust.InformationType
import com.publicrecord.common.trust.SourceQuality
import com.publicrecord.common.trust.TrustScoreInput
import com.publicrecord.common.trust.TrustScoringService
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.OfficeElectionRepository
import com.publicrecord.storage.repositories.PoliticianRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import com.publicrecord.storage.repositories.VotingRecordRepository
import java.time.LocalDate
import java.util.UUID

class PoliticianProfileService(
    private val politicianRepository: PoliticianRepository,
    private val votingRecordRepository: VotingRecordRepository,
    private val billRepository: BillRepository,
    private val contentItemRepository: ContentItemRepository,
    private val sourceCitationRepository: SourceCitationRepository,
    private val officeElectionRepository: OfficeElectionRepository
) {
    fun findProfile(politicianId: UUID): PoliticianProfileDto? {
        val politician = politicianRepository.findById(politicianId) ?: return null
        val joinedVotingRecords = votingRecordRepository.findJoinedByPoliticianId(politicianId, 250)
        val votingRecords = joinedVotingRecords.map { it.toDto() }
        val votedBills = joinedVotingRecords.mapNotNull { vote ->
            billRepository.findById(vote.billId)?.let { bill ->
                ProfileBillVoteDto(bill, vote.voteType, vote.voteDate)
            }
        }
        val billsSupported = votedBills.filter { it.voteType.equals("YEA", ignoreCase = true) }.map { it.bill }
        val billsOpposed = votedBills.filter { it.voteType.equals("NAY", ignoreCase = true) }.map { it.bill }
        val billsSponsored = billRepository.findByIntroducedBy(politicianId, 100)
        val contentItems = contentItemRepository.findByPoliticianId(politicianId, 100, 0)
        val citations = (
            sourceCitationRepository.findByTarget("POLITICIAN", politicianId, 100) +
                sourceCitationRepository.findByTarget("OFFICE", politicianId, 100)
            ).distinctBy { it.id }
        val offices = officeElectionRepository.findOfficeHistory(politicianId)
        val elections = officeElectionRepository.findElectionHistoryForPolitician(politicianId)
        val trustSummary = buildTrustSummary(
            citationCount = citations.size,
            voteCount = votingRecords.size,
            contentCount = contentItems.size
        )

        return PoliticianProfileDto(
            politician = politician,
            offices = offices,
            elections = elections,
            trustSummary = trustSummary,
            votingRecords = votingRecords,
            votedBills = votedBills,
            billsSupported = billsSupported,
            billsOpposed = billsOpposed,
            billsSponsored = billsSponsored,
            contentItems = contentItems,
            citations = citations,
            timeline = buildTimeline(votedBills, billsSponsored, contentItems)
        )
    }

    private fun buildTrustSummary(citationCount: Int, voteCount: Int, contentCount: Int): ProfileTrustSummaryDto {
        val records = listOf(
            TrustScoringService.score(
                TrustScoreInput(
                    informationType = InformationType.VOTING_RECORD,
                    sourceQuality = SourceQuality.OFFICIAL_RECORD,
                    citationCount = citationCount,
                    publishedDate = LocalDate.now()
                )
            ),
            TrustScoringService.score(
                TrustScoreInput(
                    informationType = InformationType.VERIFIED_FACT,
                    sourceQuality = if (citationCount > 0) SourceQuality.PRIMARY_SOURCE else SourceQuality.UNKNOWN,
                    citationCount = citationCount
                )
            )
        )
        val average = if (records.isEmpty()) 0.0 else records.map { it.score }.average()

        return ProfileTrustSummaryDto(
            averageScore = "%.2f".format(average).toDouble(),
            citationCount = citationCount,
            openRiskCount = listOf(voteCount == 0, contentCount == 0, citationCount == 0).count { it },
            records = records
        )
    }

    private fun buildTimeline(
        votedBills: List<ProfileBillVoteDto>,
        billsSponsored: List<com.publicrecord.common.models.Bill>,
        contentItems: List<com.publicrecord.common.models.ContentItem>
    ): List<ProfileTimelineItemDto> {
        val voteItems = votedBills.map {
            ProfileTimelineItemDto(
                date = it.voteDate.atStartOfDay(),
                category = "Vote",
                title = "${it.voteType} on ${it.bill.billNumber}",
                description = it.bill.title,
                sourceUrl = it.bill.billUrl
            )
        }
        val billItems = billsSponsored.map {
            ProfileTimelineItemDto(
                date = it.introducedDate.atStartOfDay(),
                category = "Bill",
                title = "Sponsored ${it.billNumber}",
                description = it.title,
                sourceUrl = it.billUrl
            )
        }
        val contentTimelineItems = contentItems.map {
            ProfileTimelineItemDto(
                date = it.publishedAt,
                category = it.contentType.replaceFirstChar { char -> char.uppercase() },
                title = it.title,
                description = it.textBody,
                sourceUrl = it.sourceUrl
            )
        }

        return (voteItems + billItems + contentTimelineItems)
            .sortedByDescending(ProfileTimelineItemDto::date)
            .take(100)
    }
}
