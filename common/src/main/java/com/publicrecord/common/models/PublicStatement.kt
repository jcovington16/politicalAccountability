package com.publicrecord.common.models

import java.time.LocalDateTime
import java.util.UUID

data class PublicStatement(
    val id: UUID,
    val politicianId: UUID?,
    val statementType: String,
    val title: String,
    val body: String?,
    val quote: String?,
    val venue: String?,
    val statementDate: LocalDateTime,
    val sourceCitationId: UUID?,
    val confidence: Double?
)

data class BillSponsor(
    val id: UUID,
    val billId: UUID,
    val politicianId: UUID,
    val politicianName: String,
    val party: String?,
    val state: String?,
    val sponsorType: String,
    val sponsorshipDate: java.time.LocalDate?,
    val sourceCitationId: UUID?
)
