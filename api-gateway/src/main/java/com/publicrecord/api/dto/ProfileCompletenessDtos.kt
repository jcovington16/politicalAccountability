package com.publicrecord.api.dto

data class ProfileCompletenessResponse(
    val politicianId: String,
    val score: Int,
    val status: String,
    val fields: List<ProfileCompletenessField>,
    val summary: String
)

data class ProfileCompletenessField(
    val key: String,
    val label: String,
    val count: Int,
    val complete: Boolean,
    val publicMessage: String,
    val internalNextStep: String
)
