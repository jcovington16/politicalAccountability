package com.publicrecord.api.dto

import com.publicrecord.common.models.Bill
import com.publicrecord.common.models.VotingRecord
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
    val billId: UUID,
    val voteType: String,
    val voteDate: LocalDate
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
