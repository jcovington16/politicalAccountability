package com.publicrecord.api.services

import com.publicrecord.api.dto.PublicStatementDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.PublicStatementRepository
import java.util.UUID

class PublicStatementService(private val publicStatementRepository: PublicStatementRepository) {
    fun findByPoliticianId(politicianId: UUID, limit: Int): List<PublicStatementDto> {
        return publicStatementRepository.findByPoliticianId(politicianId, limit).map { it.toDto() }
    }

    fun search(query: String?, statementType: String?, limit: Int): List<PublicStatementDto> {
        return publicStatementRepository.search(query, statementType, limit).map { it.toDto() }
    }
}
