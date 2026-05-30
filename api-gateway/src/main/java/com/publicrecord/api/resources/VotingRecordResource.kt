package com.publicrecord.api.resources

import com.publicrecord.api.services.VotingRecordService
import java.util.UUID
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
class VotingRecordResource(private val votingRecordService: VotingRecordService) {

    @GET
    @Path("/politicians/{politicianId}/votes")
    fun getVotesByPolitician(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        return try {
            Response.ok(
                votingRecordService.findByPoliticianId(UUID.fromString(politicianId), limit.coerceIn(1, 250))
            ).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/bills/{billId}/votes")
    fun getVotesByBill(
        @PathParam("billId") billId: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        return try {
            Response.ok(
                votingRecordService.findByBillId(UUID.fromString(billId), limit.coerceIn(1, 250))
            ).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }
}
