package com.publicrecord.api.resources

import com.publicrecord.api.dto.CivicClassificationRequest
import com.publicrecord.common.classification.CivicClassificationService
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/classification")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class ClassificationResource {
    @POST
    @Path("/civic")
    fun classify(request: CivicClassificationRequest?): Response {
        if (request?.text.isNullOrBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("text is required")
                .build()
        }

        return Response.ok(CivicClassificationService.classify(request!!.toInput())).build()
    }
}
