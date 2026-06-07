package com.publicrecord.api.resources

import com.publicrecord.api.services.IdentityResolutionService
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/identity")
@Produces(MediaType.APPLICATION_JSON)
class IdentityResource(private val identityResolutionService: IdentityResolutionService) {
    @GET
    @Path("/politicians/resolve")
    fun resolvePolitician(
        @QueryParam("query") query: String?,
        @QueryParam("sourceSystem") sourceSystem: String?,
        @QueryParam("externalId") externalId: String?,
        @QueryParam("state") state: String?,
        @QueryParam("party") party: String?,
        @QueryParam("office") office: String?,
        @QueryParam("limit") @DefaultValue("10") limit: Int
    ): Response {
        if (query.isNullOrBlank() && (sourceSystem.isNullOrBlank() || externalId.isNullOrBlank())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Provide either query or sourceSystem plus externalId")
                .build()
        }

        return Response.ok(
            identityResolutionService.resolvePolitician(
                query = query,
                sourceSystem = sourceSystem,
                externalId = externalId,
                state = state,
                party = party,
                office = office,
                limit = limit
            )
        ).build()
    }
}
