package com.publicrecord.api.dto

data class ReviewQueueResponse(
    val generatedAt: String,
    val total: Int,
    val items: List<ReviewQueueItem>
)

data class ReviewQueueItem(
    val targetType: String,
    val targetId: String,
    val title: String,
    val source: String? = null,
    val reason: String,
    val severity: String,
    val sourceUrl: String? = null,
    val createdAt: String? = null
)
