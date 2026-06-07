package com.publicrecord.api.resources

import com.publicrecord.api.services.BillService
import com.publicrecord.api.services.VotingRecordService
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/bills")
@Produces(MediaType.APPLICATION_JSON)
class BillResource @JvmOverloads constructor(
    private val billService: BillService,
    private val votingRecordService: VotingRecordService? = null
) {
    private val logger = LoggerFactory.getLogger(BillResource::class.java)
    private val allowedStatuses = setOf("Pending", "Passed", "Failed", "Vetoed")

    @GET
    @Path("/{id}")
    fun getBill(@PathParam("id") id: String): Response {
        return try {
            val bill = billService.findDetail(UUID.fromString(id), 100, votingRecordService)
            if (bill == null) {
                Response.status(Response.Status.NOT_FOUND).entity("Bill not found").build()
            } else {
                Response.ok(bill).build()
            }
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/{id}/actions")
    fun getBillActions(
        @PathParam("id") id: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        return try {
            Response.ok(billService.findActions(UUID.fromString(id), limit.coerceIn(1, 250))).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/{id}/citations")
    fun getBillCitations(
        @PathParam("id") id: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        return try {
            Response.ok(billService.findCitations(UUID.fromString(id), limit.coerceIn(1, 250))).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/{id}/votes")
    fun getBillVotes(
        @PathParam("id") id: String,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        return try {
            val service = votingRecordService
                ?: return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Voting record service is not configured")
                    .build()
            Response.ok(service.findByBillId(UUID.fromString(id), limit.coerceIn(1, 250))).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/search")
    fun searchBills(
        @QueryParam("query") query: String?,
        @QueryParam("status") status: String?,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        if (!status.isNullOrBlank() && status !in allowedStatuses) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("status must be one of: ${allowedStatuses.joinToString(", ")}")
                .build()
        }

        logger.info("Searching bills query={} status={} limit={}", query, status, limit)
        return Response.ok(billService.search(query, status, limit.coerceIn(1, 100))).build()
    }
}
