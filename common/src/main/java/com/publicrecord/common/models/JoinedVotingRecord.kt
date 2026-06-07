package com.publicrecord.common.models

import java.time.LocalDate
import java.util.UUID

data class JoinedVotingRecord(
    val id: UUID,
    val politicianId: UUID,
    val politicianName: String?,
    val party: String?,
    val state: String?,
    val billId: UUID,
    val billNumber: String?,
    val billTitle: String?,
    val billUrl: String?,
    val voteType: String,
    val voteDate: LocalDate
)
