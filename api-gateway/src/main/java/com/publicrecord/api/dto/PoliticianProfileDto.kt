package com.publicrecord.api.dto

import com.publicrecord.common.models.Bill
import com.publicrecord.common.models.ContentItem
import com.publicrecord.common.models.ElectionHistoryItem
import com.publicrecord.common.models.OfficeHistoryItem
import com.publicrecord.common.models.Politician
import com.publicrecord.common.models.SourceCitation
import com.publicrecord.common.trust.TrustScore
import java.time.LocalDate
import java.time.LocalDateTime

data class PoliticianProfileDto(
    val politician: Politician,
    val offices: List<OfficeHistoryItem>,
    val elections: List<ElectionHistoryItem>,
    val trustSummary: ProfileTrustSummaryDto,
    val votingRecords: List<VotingRecordDto>,
    val votedBills: List<ProfileBillVoteDto>,
    val billsSupported: List<Bill>,
    val billsOpposed: List<Bill>,
    val billsSponsored: List<Bill>,
    val contentItems: List<ContentItem>,
    val citations: List<SourceCitation>,
    val timeline: List<ProfileTimelineItemDto>
)

data class ProfileBillVoteDto(
    val bill: Bill,
    val voteType: String,
    val voteDate: LocalDate
)

data class ProfileTimelineItemDto(
    val date: LocalDateTime,
    val category: String,
    val title: String,
    val description: String?,
    val sourceUrl: String?
)

data class ProfileTrustSummaryDto(
    val averageScore: Double,
    val citationCount: Int,
    val openRiskCount: Int,
    val records: List<TrustScore>
)
