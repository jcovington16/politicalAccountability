package com.publicrecord.api.dto

import com.publicrecord.common.models.Bill
import com.publicrecord.common.models.BillAction
import com.publicrecord.common.models.BillSponsor
import com.publicrecord.common.models.JoinedVotingRecord
import com.publicrecord.common.models.PublicStatement
import com.publicrecord.common.models.SourceCitation
import com.publicrecord.common.models.VotingRecord
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID

data class BillDto(
    val id: UUID,
    val billNumber: String,
    val title: String,
    val description: String?,
    val introducedBy: UUID?,
    val status: String,
    val introducedDate: LocalDate,
    val lastActionDate: LocalDate?,
    val billUrl: String?
)

data class VotingRecordDto(
    val id: UUID,
    val politicianId: UUID,
    val politicianName: String? = null,
    val party: String? = null,
    val state: String? = null,
    val billId: UUID,
    val billNumber: String? = null,
    val billTitle: String? = null,
    val billUrl: String? = null,
    val voteType: String,
    val voteDate: LocalDate
)

data class BillSponsorDto(
    val id: UUID,
    val billId: UUID,
    val politicianId: UUID,
    val politicianName: String,
    val party: String?,
    val state: String?,
    val sponsorType: String,
    val sponsorshipDate: LocalDate?,
    val sourceCitationId: UUID?
)

data class BillDetailDto(
    val bill: BillDto,
    val sponsors: List<BillSponsorDto>,
    val cosponsors: List<BillSponsorDto>,
    val actions: List<BillAction>,
    val citations: List<SourceCitation>,
    val votes: List<VotingRecordDto>
)

data class PublicStatementDto(
    val id: UUID,
    val politicianId: UUID?,
    val statementType: String,
    val title: String,
    val body: String?,
    val quote: String?,
    val venue: String?,
    val statementDate: LocalDateTime,
    val sourceCitationId: UUID?,
    val confidence: Double?,
    val suspiciousContent: Boolean
)

fun Bill.toDto(): BillDto = BillDto(
    id = id,
    billNumber = billNumber,
    title = title,
    description = description,
    introducedBy = introducedBy,
    status = status,
    introducedDate = introducedDate,
    lastActionDate = lastActionDate,
    billUrl = billUrl
)

fun VotingRecord.toDto(): VotingRecordDto = VotingRecordDto(
    id = id,
    politicianId = politicianId,
    billId = billId,
    voteType = voteType,
    voteDate = voteDate
)

fun JoinedVotingRecord.toDto(): VotingRecordDto = VotingRecordDto(
    id = id,
    politicianId = politicianId,
    politicianName = politicianName,
    party = party,
    state = state,
    billId = billId,
    billNumber = billNumber,
    billTitle = billTitle,
    billUrl = billUrl,
    voteType = voteType,
    voteDate = voteDate
)

fun BillSponsor.toDto(): BillSponsorDto = BillSponsorDto(
    id = id,
    billId = billId,
    politicianId = politicianId,
    politicianName = politicianName,
    party = party,
    state = state,
    sponsorType = sponsorType,
    sponsorshipDate = sponsorshipDate,
    sourceCitationId = sourceCitationId
)

fun PublicStatement.toDto(): PublicStatementDto = PublicStatementDto(
    id = id,
    politicianId = politicianId,
    statementType = statementType,
    title = title,
    body = body,
    quote = quote,
    venue = venue,
    statementDate = statementDate,
    sourceCitationId = sourceCitationId,
    confidence = confidence,
    suspiciousContent = listOfNotNull(title, body, quote).any { text ->
        val normalized = text.lowercase()
        listOf("ignore previous instructions", "system prompt", "developer message", "act as", "jailbreak").any(normalized::contains)
    }
)
