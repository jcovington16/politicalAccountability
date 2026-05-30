package com.publicrecord.common.models

import java.time.LocalDate
import java.util.UUID

data class Bill(
    val id: UUID = UUID.randomUUID(),
    val billNumber: String,
    val title: String,
    val description: String?,
    val introducedBy: UUID?,
    val status: String,
    val introducedDate: LocalDate,
    val lastActionDate: LocalDate?,
    val billUrl: String?,
)
