package com.publicrecord.api.services

import com.publicrecord.api.dto.BillDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.BillRepository
import java.util.UUID

class BillService(private val billRepository: BillRepository) {
    fun findById(id: UUID): BillDto? = billRepository.findById(id)?.toDto()

    fun search(query: String?, status: String?, limit: Int): List<BillDto> {
        return billRepository.search(query, status, limit).map { it.toDto() }
    }
}
