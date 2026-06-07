package com.publicrecord.api.resources

import com.publicrecord.api.services.SearchService
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
class SearchResource(private val searchService: SearchService) {
    @GET
    fun search(
        @QueryParam("query") query: String?,
        @QueryParam("limit") @DefaultValue("10") limit: Int
    ): Response {
        if (query.isNullOrBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("query is required").build()
        }
        return Response.ok(searchService.search(query.trim(), limit)).build()
    }
}
