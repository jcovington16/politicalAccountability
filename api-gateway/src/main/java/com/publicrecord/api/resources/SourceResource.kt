package com.publicrecord.api.resources

import com.publicrecord.api.services.SourceService
import java.util.UUID
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
class SourceResource(private val sourceService: SourceService) {
    private val allowedCitationTypes = setOf("BILL", "VOTE", "STATEMENT", "CLAIM", "FACT_CHECK", "NEWS_ARTICLE", "CONTENT_ITEM", "OFFICE", "ELECTION")
    private val allowedQualities = setOf("OFFICIAL_RECORD", "PRIMARY_SOURCE", "REPUTABLE_NEWS", "ADVOCACY_OR_PARTISAN", "SOCIAL_MEDIA", "UNKNOWN")
    private val allowedSourceTypes = allowedQualities

    @GET
    @Path("/citations")
    fun searchCitations(
        @QueryParam("type") citationType: String?,
        @QueryParam("quality") sourceQuality: String?,
        @QueryParam("query") query: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        val validation = validateCitation(citationType, sourceQuality)
        if (validation != null) return validation
        return Response.ok(sourceService.searchCitations(citationType, sourceQuality, query, limit.coerceIn(1, 250))).build()
    }

    @GET
    @Path("/citations/{citationType}/{targetId}")
    fun getTargetCitations(
        @PathParam("citationType") citationType: String,
        @PathParam("targetId") targetId: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        val validation = validateCitation(citationType, null)
        if (validation != null) return validation
        return try {
            Response.ok(sourceService.findByTarget(citationType, UUID.fromString(targetId), limit.coerceIn(1, 250))).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/sources")
    fun getSources(
        @QueryParam("type") sourceType: String?,
        @QueryParam("query") query: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        if (!sourceType.isNullOrBlank() && sourceType !in allowedSourceTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("type must be one of: ${allowedSourceTypes.joinToString(", ")}")
                .build()
        }
        return Response.ok(sourceService.findSources(sourceType, query, limit.coerceIn(1, 250))).build()
    }

    private fun validateCitation(citationType: String?, sourceQuality: String?): Response? {
        if (!citationType.isNullOrBlank() && citationType !in allowedCitationTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("type must be one of: ${allowedCitationTypes.joinToString(", ")}")
                .build()
        }
        if (!sourceQuality.isNullOrBlank() && sourceQuality !in allowedQualities) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("quality must be one of: ${allowedQualities.joinToString(", ")}")
                .build()
        }
        return null
    }
}
