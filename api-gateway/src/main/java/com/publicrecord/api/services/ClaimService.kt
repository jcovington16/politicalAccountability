package com.publicrecord.api.services

import com.publicrecord.api.dto.ClaimDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.ClaimRepository
import java.util.UUID

class ClaimService(private val claimRepository: ClaimRepository) {
    fun findByPoliticianId(politicianId: UUID, claimType: String?, status: String?, limit: Int): List<ClaimDto> {
        return claimRepository.findByPoliticianId(politicianId, claimType, status, limit).map { claim ->
            claim.toDto(claimRepository.findFactChecks(claim.id))
        }
    }

    fun search(query: String?, claimType: String?, status: String?, limit: Int): List<ClaimDto> {
        return claimRepository.search(query, claimType, status, limit).map { claim ->
            claim.toDto(claimRepository.findFactChecks(claim.id))
        }
    }
}
