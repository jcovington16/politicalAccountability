package com.publicrecord.api.resources

import com.publicrecord.storage.repositories.AuditLogRepository
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/audit-log")
@Produces(MediaType.APPLICATION_JSON)
class AuditLogResource(private val auditLogRepository: AuditLogRepository) {
    @GET
    fun getAuditLog(@QueryParam("limit") @DefaultValue("100") limit: Int): Response {
        return Response.ok(auditLogRepository.findRecent(limit)).build()
    }
}
