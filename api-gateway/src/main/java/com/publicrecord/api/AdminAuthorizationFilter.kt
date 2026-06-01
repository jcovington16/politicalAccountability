package com.publicrecord.api

import java.security.MessageDigest
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHENTICATION)
class AdminAuthorizationFilter(private val adminApiToken: String) : ContainerRequestFilter {
    private val protectedPrefixes = listOf("imports", "audit-log")

    override fun filter(requestContext: ContainerRequestContext) {
        val path = requestContext.uriInfo.path.trimStart('/')
        if (protectedPrefixes.none { path == it || path.startsWith("$it/") }) {
            return
        }

        if (adminApiToken.isBlank()) {
            requestContext.abortWith(
                Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Admin API token is not configured")
                    .build()
            )
            return
        }

        val provided = requestContext.getHeaderString(ADMIN_TOKEN_HEADER)
        if (!constantTimeEquals(adminApiToken, provided)) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Admin authorization required")
                    .build()
            )
        }
    }

    private fun constantTimeEquals(expected: String, actual: String?): Boolean {
        if (actual == null) return false
        return MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())
    }

    companion object {
        const val ADMIN_TOKEN_HEADER = "X-Admin-Token"
    }
}
