package com.publicrecord.common.models

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ExternalIdentifier(
    val id: UUID,
    val entityType: String,
    val entityId: UUID,
    val sourceSystem: String,
    val externalId: String,
    val sourceUrl: String?,
    val confidence: BigDecimal?,
    val metadata: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class SocialAccount(
    val id: UUID,
    val politicianId: UUID,
    val platform: String,
    val handle: String,
    val accountUrl: String,
    val displayName: String?,
    val verificationStatus: String,
    val sourceCitationId: UUID?,
    val confidence: BigDecimal,
    val metadata: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class IdentityMatchCandidate(
    val politician: Politician,
    val confidence: BigDecimal,
    val reasons: List<String>,
    val externalIdentifiers: List<ExternalIdentifier>,
    val socialAccounts: List<SocialAccount>
)

data class IdentityResolutionResult(
    val query: String?,
    val sourceSystem: String?,
    val externalId: String?,
    val candidates: List<IdentityMatchCandidate>,
    val needsReview: Boolean,
    val reviewReason: String?
)
