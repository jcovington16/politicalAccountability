package com.publicrecord.api.resources

import com.publicrecord.storage.repositories.ImportRepository
import java.util.UUID
import javax.ws.rs.DefaultValue
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/imports")
@Produces(MediaType.APPLICATION_JSON)
class ImportResource(private val importRepository: ImportRepository) {
    private val allowedBatchStatuses = setOf("STARTED", "COMPLETED", "FAILED")
    private val allowedRowStatuses = setOf("IMPORTED", "SKIPPED", "FAILED")

    @GET
    fun getImports(
        @QueryParam("status") status: String?,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        val normalizedStatus = status?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (normalizedStatus != null && normalizedStatus !in allowedBatchStatuses) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("status must be one of: ${allowedBatchStatuses.joinToString(", ")}")
                .build()
        }
        return Response.ok(importRepository.findBatches(normalizedStatus, limit)).build()
    }

    @GET
    @Path("/{id}")
    fun getImport(@PathParam("id") id: String): Response {
        return try {
            val batch = importRepository.findBatch(UUID.fromString(id))
            if (batch == null) Response.status(Response.Status.NOT_FOUND).entity("Import batch not found").build()
            else Response.ok(batch).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }

    @GET
    @Path("/{id}/rows")
    fun getImportRows(
        @PathParam("id") id: String,
        @QueryParam("status") status: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        val normalizedStatus = status?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
        if (normalizedStatus != null && normalizedStatus !in allowedRowStatuses) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("status must be one of: ${allowedRowStatuses.joinToString(", ")}")
                .build()
        }

        return try {
            Response.ok(importRepository.findRows(UUID.fromString(id), normalizedStatus, limit)).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST).entity("Invalid UUID format").build()
        }
    }
}
