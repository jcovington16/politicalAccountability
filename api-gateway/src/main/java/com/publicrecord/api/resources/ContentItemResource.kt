package com.publicrecord.api.resources

import com.publicrecord.storage.repositories.ContentItemRepository
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * API resource for content item operations
 */
@Path("/content")
@Produces(MediaType.APPLICATION_JSON)
class ContentItemResource(
    private val repository: ContentItemRepository
) {

    private val logger = LoggerFactory.getLogger(ContentItemResource::class.java)

    /**
     * Get content item by ID
     * Example: GET /content/{id}
     */
    @GET
    @Path("/{id}")
    fun getContentItem(@PathParam("id") id: String): Response {
        logger.info("Fetching content item with ID: $id")
        
        return try {
            val uuid = UUID.fromString(id)
            val contentItem = repository.findById(uuid)
            if (contentItem != null) {
                Response.ok(contentItem).build()
            } else {
                Response.status(Response.Status.NOT_FOUND)
                    .entity("Content item not found").build()
            }
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        }
    }

    /**
     * Get content by politician
     * Example: GET /content/politician/550e8400-e29b-41d4-a716-446655440000?limit=50&offset=0
     */
    @GET
    @Path("/politician/{politicianId}")
    fun getContentByPolitician(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int
    ): Response {
        logger.info("Fetching content for politician: $politicianId (limit=$limit, offset=$offset)")
        
        return try {
            val uuid = UUID.fromString(politicianId)
            val contentItems = repository.findByPoliticianId(uuid, limit, offset)
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        } catch (e: Exception) {
            logger.error("Error fetching content: ${e.message}", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error fetching content").build()
        }
    }

    /**
     * Get content by type for a politician
     * Example: GET /content/politician/550e8400-e29b-41d4-a716-446655440000/type/tweet
     */
    @GET
    @Path("/politician/{politicianId}/type/{contentType}")
    fun getContentByType(
        @PathParam("politicianId") politicianId: String,
        @PathParam("contentType") contentType: String,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        logger.info("Fetching $contentType content for politician: $politicianId")
        
        return try {
            val uuid = UUID.fromString(politicianId)
            val contentItems = repository.findByContentType(contentType, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        } catch (e: Exception) {
            logger.error("Error fetching content by type: ${e.message}", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error fetching content").build()
        }
    }

    /**
     * Search content for a politician
     * Example: GET /content/politician/550e8400-e29b-41d4-a716-446655440000/search?keyword=healthcare
     */
    @GET
    @Path("/politician/{politicianId}/search")
    fun searchContent(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("keyword") keyword: String?,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        if (keyword.isNullOrBlank()) {
            logger.error("Keyword parameter is missing")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Keyword query parameter is required").build()
        }

        logger.info("Searching content for politician: $politicianId with keyword: $keyword")
        
        return try {
            val uuid = UUID.fromString(politicianId)
            val contentItems = repository.searchByKeyword(keyword, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        } catch (e: Exception) {
            logger.error("Error searching content: ${e.message}", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error searching content").build()
        }
    }

    /**
     * Get content by date range
     * Example: GET /content/politician/550e8400-e29b-41d4-a716-446655440000/daterange?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GET
    @Path("/politician/{politicianId}/daterange")
    fun getContentByDateRange(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) {
            logger.error("Date range parameters are missing")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("startDate and endDate parameters are required").build()
        }

        return try {
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val start = LocalDateTime.parse(startDate, formatter)
            val end = LocalDateTime.parse(endDate, formatter)
            val uuid = UUID.fromString(politicianId)
            
            logger.info("Fetching content for politician: $politicianId from $start to $end")
            val contentItems = repository.findByDateRange(start, end, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        } catch (e: Exception) {
            logger.error("Error fetching content by date range: ${e.message}", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error fetching content").build()
        }
    }

    /**
     * Get timeline statistics
     * Example: GET /content/politician/550e8400-e29b-41d4-a716-446655440000/stats
     */
    @GET
    @Path("/politician/{politicianId}/stats")
    fun getTimelineStats(
        @PathParam("politicianId") politicianId: String
    ): Response {
        logger.info("Fetching timeline statistics for politician: $politicianId")
        
        return try {
            val stats = mapOf(
                "politicianId" to politicianId,
                "lastUpdated" to LocalDateTime.now().toString(),
                "message" to "Statistics endpoint - implement actual stats calculation"
            )
            Response.ok(stats).build()
        } catch (e: Exception) {
            logger.error("Error fetching statistics: ${e.message}", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error fetching statistics").build()
        }
    }
}
