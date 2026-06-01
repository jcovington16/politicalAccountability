package com.publicrecord.common.models

import java.time.LocalDateTime
import java.util.UUID

data class ImportBatch(
    val id: UUID,
    val sourceSystem: String,
    val sourceDetail: String?,
    val status: String,
    val startedAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val recordsSeen: Int,
    val recordsImported: Int,
    val recordsSkipped: Int,
    val errorMessage: String?,
    val sourceChecksum: String?,
    val metadata: String
)

data class ImportRowResult(
    val id: UUID,
    val importBatchId: UUID,
    val sourceRecordId: String?,
    val targetType: String?,
    val targetId: UUID?,
    val status: String,
    val message: String?,
    val rowPayload: String?,
    val createdAt: LocalDateTime
)

data class AuditLogEntry(
    val id: UUID,
    val actorType: String,
    val actorId: UUID?,
    val action: String,
    val targetType: String,
    val targetId: UUID?,
    val sourceSystem: String?,
    val importBatchId: UUID?,
    val requestId: String?,
    val metadata: String,
    val createdAt: LocalDateTime
)
