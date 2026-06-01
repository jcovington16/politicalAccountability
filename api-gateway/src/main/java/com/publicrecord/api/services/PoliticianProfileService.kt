package com.publicrecord.api.services

import com.publicrecord.api.dto.PoliticianProfileDto
import com.publicrecord.api.dto.ProfileBillVoteDto
import com.publicrecord.api.dto.ProfileTimelineItemDto
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.PoliticianRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import com.publicrecord.storage.repositories.VotingRecordRepository
import java.time.LocalDateTime
import java.util.UUID

class PoliticianProfileService(
    private val politicianRepository: PoliticianRepository,
    private val votingRecordRepository: VotingRecordRepository,
    private val billRepository: BillRepository,
    private val contentItemRepository: ContentItemRepository,
    private val sourceCitationRepository: SourceCitationRepository
) {
    fun findProfile(politicianId: UUID): PoliticianProfileDto? {
        val politician = politicianRepository.findById(politicianId) ?: return null
        val votingRecords = votingRecordRepository.findByPoliticianId(politicianId, 250)
        val votedBills = votingRecords.mapNotNull { vote ->
            billRepository.findById(vote.billId)?.let { bill ->
                ProfileBillVoteDto(bill, vote.voteType, vote.voteDate)
            }
        }
        val billsSupported = votedBills.filter { it.voteType.equals("YEA", ignoreCase = true) }.map { it.bill }
        val billsOpposed = votedBills.filter { it.voteType.equals("NAY", ignoreCase = true) }.map { it.bill }
        val billsSponsored = billRepository.findByIntroducedBy(politicianId, 100)
        val contentItems = contentItemRepository.findByPoliticianId(politicianId, 100, 0)
        val citations = sourceCitationRepository.findByTarget("OFFICE", politicianId, 100)

        return PoliticianProfileDto(
            politician = politician,
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
