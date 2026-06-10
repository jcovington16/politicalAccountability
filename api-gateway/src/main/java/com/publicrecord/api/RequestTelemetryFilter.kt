package com.publicrecord.api

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter

class RequestTelemetryFilter : ContainerResponseFilter {
    override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        RequestTelemetry.record(requestContext.uriInfo.path, responseContext.status)
    }
}
