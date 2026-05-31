package com.publicrecord.api.services

import com.publicrecord.api.dto.BillDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import java.util.UUID

class BillService(
    private val billRepository: BillRepository,
    private val sourceCitationRepository: SourceCitationRepository
) {
    fun findById(id: UUID): BillDto? = billRepository.findById(id)?.toDto()

    fun search(query: String?, status: String?, limit: Int): List<BillDto> {
        return billRepository.search(query, status, limit).map { it.toDto() }
    }

    fun findActions(billId: UUID, limit: Int) = billRepository.findActions(billId, limit)

    fun findCitations(billId: UUID, limit: Int) = sourceCitationRepository.findByTarget("BILL", billId, limit)
}
