package com.publicrecord.api.services

import com.publicrecord.api.dto.ProfileCompletenessField
import com.publicrecord.api.dto.ProfileCompletenessResponse
import java.util.UUID

class ProfileCompletenessService(
    private val profileService: PoliticianProfileService,
    private val timelineService: TimelineService
) {
    fun score(politicianId: UUID): ProfileCompletenessResponse? {
        val profile = profileService.findProfile(politicianId) ?: return null
        val timeline = timelineService.aggregate(politicianId, null, 250)
        val fields = listOf(
            field("biography", "Biography", if (profile.politician.biography.isNullOrBlank()) 0 else 1, "Biography has not been imported yet.", "Import official biography and profile photo."),
            field("offices", "Office history", profile.offices.size, "Office history is incomplete.", "Import office terms from Congress.gov, Open States, Google Civic, or election sources."),
            field("elections", "Election history", profile.elections.size, "Election history is incomplete.", "Import ballot and election records for this jurisdiction."),
            field("votes", "Voting record", profile.votingRecords.size, "Voting history has not been attached yet.", "Normalize roll-call votes for this politician."),
            field("sponsoredBills", "Sponsored legislation", profile.billsSponsored.size, "Sponsored legislation has not been attached yet.", "Import sponsor/cosponsor records."),
            field("statements", "Public statements", timeline.stats.byCategory["Statement"] ?: 0, "Public statements have not been reviewed yet.", "Import direct quotes, hearings, press releases, and social posts with citations."),
            field("claims", "Claims and fact checks", timeline.stats.byCategory["Claim"] ?: 0, "No reviewed claim records are attached yet.", "Attach fact checks and reviewed claims only with source context."),
            field("media", "Articles and media", mediaCount(timeline.stats.byCategory), "Recent media has not been reviewed yet.", "Run bounded media ingestion and link reviewed records."),
            field("citations", "Source citations", profile.citations.size, "Source citations are missing.", "Attach official, primary, or reputable news citations."),
            field("recentActivity", "Recent activity", timeline.stats.total, "Timeline has no linked activity yet.", "Link votes, bills, statements, media, offices, and election events.")
        )
        val score = ((fields.count { it.complete }.toDouble() / fields.size) * 100).toInt()
        val status = when {
            score >= 80 -> "demo-ready"
            score >= 50 -> "partial"
            else -> "needs-data"
        }
        val missing = fields.filterNot { it.complete }.joinToString(", ") { it.label.lowercase() }
        val summary = if (missing.isBlank()) {
            "This profile has the core public-record categories needed for a strong demo."
        } else {
            "This profile is missing or light on: $missing."
        }
        return ProfileCompletenessResponse(politicianId.toString(), score, status, fields, summary)
    }

    private fun field(key: String, label: String, count: Int, publicMessage: String, internalNextStep: String): ProfileCompletenessField {
        return ProfileCompletenessField(
            key = key,
            label = label,
            count = count,
            complete = count > 0,
            publicMessage = if (count > 0) "Available public records are linked." else publicMessage,
            internalNextStep = internalNextStep
        )
    }

    private fun mediaCount(categories: Map<String, Int>): Int {
        return categories
            .filterKeys { key -> key.equals("Article", ignoreCase = true) || key.equals("Video", ignoreCase = true) || key.equals("Social_post", ignoreCase = true) || key.equals("Social Post", ignoreCase = true) }
            .values
            .sum()
    }
}
