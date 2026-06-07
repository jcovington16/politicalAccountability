package com.publicrecord.api.dto

import java.time.LocalDateTime
import java.util.UUID

data class TimelineAggregateDto(
    val politicianId: UUID,
    val generatedAt: LocalDateTime,
    val stats: TimelineStatsDto,
    val items: List<TimelineItemDto>
)

data class TimelineStatsDto(
    val total: Int,
    val byCategory: Map<String, Int>,
    val publishableCount: Int,
    val reviewRequiredCount: Int,
    val latestActivityAt: LocalDateTime?
)

data class TimelineItemDto(
    val id: String,
    val date: LocalDateTime,
    val category: String,
    val title: String,
    val description: String?,
    val sourceUrl: String?,
    val sourceName: String?,
    val targetType: String,
    val targetId: UUID?,
    val evidenceType: String,
    val publishable: Boolean,
    val warnings: List<String> = emptyList()
)
