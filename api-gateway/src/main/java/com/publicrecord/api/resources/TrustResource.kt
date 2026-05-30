package com.publicrecord.api.resources

import com.publicrecord.api.dto.TrustScoreRequest
import com.publicrecord.common.trust.TrustScoreInput
import com.publicrecord.common.trust.TrustScoringService
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/trust")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
class TrustResource {

    @POST
    @Path("/score")
    fun score(request: TrustScoreRequest?): Response {
        if (request?.informationType == null || request.sourceQuality == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("informationType and sourceQuality are required")
                .build()
        }

        val score = TrustScoringService.score(
            TrustScoreInput(
                informationType = request.informationType,
                sourceQuality = request.sourceQuality,
                citationCount = request.citationCount,
                publishedDate = request.publishedDate
            )
        )
        return Response.ok(score).build()
    }
}
