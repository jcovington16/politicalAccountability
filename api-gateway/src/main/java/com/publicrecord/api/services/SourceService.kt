package com.publicrecord.api.services

import com.publicrecord.api.dto.CitationDto
import com.publicrecord.api.dto.SourceRegistryDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.SourceCitationRepository
import java.util.UUID

class SourceService(private val sourceCitationRepository: SourceCitationRepository) {
    fun searchCitations(citationType: String?, sourceQuality: String?, query: String?, limit: Int): List<CitationDto> {
        return sourceCitationRepository.search(citationType, sourceQuality, query, limit).map { it.toDto() }
    }

    fun findByTarget(citationType: String, targetId: UUID, limit: Int): List<CitationDto> {
        return sourceCitationRepository.findByTarget(citationType, targetId, limit).map { it.toDto() }
    }

    fun findSources(sourceType: String?, query: String?, limit: Int): List<SourceRegistryDto> {
        return sourceCitationRepository.findSources(sourceType, query, limit).map { it.toDto() }
    }
}
