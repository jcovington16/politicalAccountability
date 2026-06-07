package com.publicrecord.api.resources

import com.publicrecord.api.services.ProfileCompletenessService
import com.publicrecord.api.services.ReviewQueueService
import java.util.UUID
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/review")
@Produces(MediaType.APPLICATION_JSON)
class ReviewResource(
    private val reviewQueueService: ReviewQueueService,
    private val profileCompletenessService: ProfileCompletenessService
) {
    @GET
    @Path("/queue")
    fun queue(@QueryParam("limit") @DefaultValue("50") limit: Int): Response {
        return Response.ok(reviewQueueService.queue(limit)).build()
    }

    @GET
    @Path("/politicians/{id}/completeness")
    fun completeness(@PathParam("id") id: String): Response {
        return try {
            val result = profileCompletenessService.score(UUID.fromString(id))
            if (result == null) {
                Response.status(Response.Status.NOT_FOUND).entity("Politician not found").build()
            } else {
                Response.ok(result).build()
            }
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }
}
