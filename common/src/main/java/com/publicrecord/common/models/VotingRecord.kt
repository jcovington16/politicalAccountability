package com.publicrecord.common.models

import java.time.LocalDate
import java.util.UUID

data class VotingRecord(
    val id: UUID = UUID.randomUUID(),
    val politicianId: UUID,
    val billId: UUID,
    val voteType: String,
    val voteDate: LocalDate
)
