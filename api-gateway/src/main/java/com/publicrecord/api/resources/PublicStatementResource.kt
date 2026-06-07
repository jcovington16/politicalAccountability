package com.publicrecord.api.resources

import com.publicrecord.api.services.PublicStatementService
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/statements")
@Produces(MediaType.APPLICATION_JSON)
class PublicStatementResource(private val publicStatementService: PublicStatementService) {
    private val allowedTypes = setOf("SPEECH", "INTERVIEW", "SOCIAL", "PRESS_RELEASE", "DEBATE", "HEARING", "OTHER")

    @GET
    fun searchStatements(
        @QueryParam("query") query: String?,
        @QueryParam("type") statementType: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        if (!statementType.isNullOrBlank() && statementType !in allowedTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("type must be one of: ${allowedTypes.joinToString(", ")}")
                .build()
        }
        return Response.ok(publicStatementService.search(query, statementType, limit.coerceIn(1, 250))).build()
    }
}
