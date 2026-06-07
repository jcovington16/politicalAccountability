package com.publicrecord.api.resources

import com.publicrecord.api.services.ClaimService
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
class ClaimResource(private val claimService: ClaimService) {
    private val allowedTypes = setOf("VERIFIED_FACT", "DIRECT_QUOTE", "VOTING_RECORD", "ALLEGATION", "OPINION_PIECE", "UNRESOLVED_CLAIM")
    private val allowedStatuses = setOf("VERIFIED", "DISPUTED", "UNRESOLVED", "RETRACTED")

    @GET
    @Path("/claims")
    fun searchClaims(
        @QueryParam("query") query: String?,
        @QueryParam("type") claimType: String?,
        @QueryParam("status") status: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        val validation = validate(claimType, status)
        if (validation != null) return validation
        return Response.ok(claimService.search(query, claimType, status, limit.coerceIn(1, 250))).build()
    }

    @GET
    @Path("/politicians/{politicianId}/claims")
    fun getPoliticianClaims(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("type") claimType: String?,
        @QueryParam("status") status: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        val validation = validate(claimType, status)
        if (validation != null) return validation
        return try {
            Response.ok(claimService.findByPoliticianId(UUID.fromString(politicianId), claimType, status, limit.coerceIn(1, 250))).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    private fun validate(claimType: String?, status: String?): Response? {
        if (!claimType.isNullOrBlank() && claimType !in allowedTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("type must be one of: ${allowedTypes.joinToString(", ")}")
                .build()
        }
        if (!status.isNullOrBlank() && status !in allowedStatuses) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("status must be one of: ${allowedStatuses.joinToString(", ")}")
                .build()
        }
        return null
    }
}
