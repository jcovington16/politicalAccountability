package com.publicrecord.api.services

import com.publicrecord.api.dto.VotingRecordDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.VotingRecordRepository
import java.util.UUID

class VotingRecordService(private val votingRecordRepository: VotingRecordRepository) {
    fun findByPoliticianId(politicianId: UUID, limit: Int): List<VotingRecordDto> {
        return votingRecordRepository.findByPoliticianId(politicianId, limit).map { it.toDto() }
    }

    fun findByBillId(billId: UUID, limit: Int): List<VotingRecordDto> {
        return votingRecordRepository.findByBillId(billId, limit).map { it.toDto() }
    }
}
