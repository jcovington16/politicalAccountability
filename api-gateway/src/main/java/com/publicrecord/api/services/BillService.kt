package com.publicrecord.api.services

import com.publicrecord.api.dto.BillDetailDto
import com.publicrecord.api.dto.BillDto
import com.publicrecord.api.dto.toDto
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import java.util.UUID

class BillService @JvmOverloads constructor(
    private val billRepository: BillRepository,
    private val sourceCitationRepository: SourceCitationRepository,
    private val congressBackfillService: CongressBillBackfillService? = null
) {
    fun findById(id: UUID): BillDto? = billRepository.findById(id)?.toDto()

    fun findDetail(id: UUID, limit: Int, votingRecordService: VotingRecordService?): BillDetailDto? {
        val bill = billRepository.findById(id) ?: return null
        val sponsors = billRepository.findSponsors(id).map { it.toDto() }
        return BillDetailDto(
            bill = bill.toDto(),
            sponsors = sponsors.filter { it.sponsorType == "SPONSOR" },
            cosponsors = sponsors.filter { it.sponsorType == "COSPONSOR" },
            actions = billRepository.findActions(id, limit),
            citations = sourceCitationRepository.findByTarget("BILL", id, limit),
            votes = votingRecordService?.findByBillId(id, limit) ?: emptyList()
        )
    }

    fun search(query: String?, status: String?, limit: Int): List<BillDto> {
        val local = billRepository.searchWithSponsor(query, status, limit)
        if (local.isNotEmpty() || query.isNullOrBlank()) {
            return local.map { it.toDto() }
        }

        congressBackfillService?.backfill(query, limit)
        return billRepository.searchWithSponsor(query, status, limit).map { it.toDto() }
    }

    fun findActions(billId: UUID, limit: Int) = billRepository.findActions(billId, limit)

    fun findCitations(billId: UUID, limit: Int) = sourceCitationRepository.findByTarget("BILL", billId, limit)
}
