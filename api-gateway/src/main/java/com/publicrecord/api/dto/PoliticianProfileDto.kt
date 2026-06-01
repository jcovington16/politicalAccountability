package com.publicrecord.api.dto

import com.publicrecord.common.models.Bill
import com.publicrecord.common.models.ContentItem
import com.publicrecord.common.models.Politician
import com.publicrecord.common.models.SourceCitation
import com.publicrecord.common.models.VotingRecord
import java.time.LocalDate
import java.time.LocalDateTime

data class PoliticianProfileDto(
    val politician: Politician,
    val votingRecords: List<VotingRecord>,
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
