package com.publicrecord.common.models

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class OfficeHistoryItem(
    val officeId: UUID,
    val title: String,
    val branch: String,
    val officeLevel: String,
    val jurisdiction: String,
    val state: String?,
    val district: String?,
    val seatIdentifier: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isCurrent: Boolean,
    val sourceUrl: String
)

data class ElectionCandidateItem(
    val politicianId: UUID,
    val fullName: String,
    val party: String?,
    val ballotStatus: String,
    val resultStatus: String?,
    val voteTotal: Int?,
    val votePercentage: BigDecimal?,
    val sourceUrl: String
)

data class ElectionHistoryItem(
    val electionId: UUID,
    val officeId: UUID,
    val electionDate: LocalDate,
    val electionType: String,
    val cycleYear: Int,
    val jurisdiction: String,
    val sourceUrl: String,
    val candidates: List<ElectionCandidateItem>
)
