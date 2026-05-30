package com.publicrecord.api.resources

import org.slf4j.LoggerFactory
import com.publicrecord.storage.repositories.ContentItemRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * API resource for fetching politician timelines and content
 */
@Path("/politicians/{politicianId}/timeline")
@Produces(MediaType.APPLICATION_JSON)
class TimelineResource(
    private val contentItemRepository: ContentItemRepository
) {

    private val logger = LoggerFactory.getLogger(TimelineResource::class.java)

    /**
     * Get politician's content timeline
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000/timeline
     */
    @GET
    fun getTimeline(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("limit") @DefaultValue("50") limit: Int,
        @QueryParam("offset") @DefaultValue("0") offset: Int
    ): Response {
        logger.info("Fetching timeline for politician: $politicianId (limit=$limit, offset=$offset)")

        return try {
            val contentItems = contentItemRepository.findByPoliticianId(
                UUID.fromString(politicianId),
                limit,
                offset
            )
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        }
    }

    /**
     * Filter timeline by content type
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000/timeline?contentType=tweet
     */
    @GET
    @Path("/filter")
    fun filterTimeline(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("contentType") contentType: String,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        logger.info("Filtering timeline for politician: $politicianId by type: $contentType")

        return try {
            val uuid = UUID.fromString(politicianId)
            val contentItems = contentItemRepository.findByContentType(contentType, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        }
    }

    /**
     * Search timeline by keyword
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000/timeline/search?keyword=healthcare
     */
    @GET
    @Path("/search")
    fun searchTimeline(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("keyword") keyword: String?,
        @QueryParam("limit") @DefaultValue("50") limit: Int
    ): Response {
        if (keyword.isNullOrBlank()) {
            logger.error("Keyword parameter is missing")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Keyword query parameter is required").build()
        }
        
        return try {
            val uuid = UUID.fromString(politicianId)
            logger.info("Searching timeline for politician: $politicianId with keyword: $keyword")
            val contentItems = contentItemRepository.searchByKeyword(keyword, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        }
    }

    /**
     * Get timeline by date range
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000/timeline/daterange?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
     */
    @GET
    @Path("/daterange")
    fun getTimelineByDateRange(
        @PathParam("politicianId") politicianId: String,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
        @QueryParam("limit") @DefaultValue("100") limit: Int
    ): Response {
        if (startDate.isNullOrBlank() || endDate.isNullOrBlank()) {
            logger.error("Date range parameters are missing")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("startDate and endDate parameters are required (ISO format)").build()
        }

        return try {
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val start = LocalDateTime.parse(startDate, formatter)
            val end = LocalDateTime.parse(endDate, formatter)
            val uuid = UUID.fromString(politicianId)
            
            logger.info("Fetching timeline for politician: $politicianId from $start to $end")
            val contentItems = contentItemRepository.findByDateRange(start, end, limit)
                .filter { it.politicianId == uuid }
            Response.ok(contentItems).build()
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        } catch (e: Exception) {
            logger.error("Invalid date format: ${e.message}")
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid date format. Use ISO 8601 format (e.g., 2024-01-01T00:00:00)").build()
        }
    }

    /**
     * Get timeline statistics
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000/timeline/stats
     */
    @GET
    @Path("/stats")
    fun getTimelineStats(
        @PathParam("politicianId") politicianId: String
    ): Response {
        logger.info("Fetching timeline statistics for politician: $politicianId")

        val stats = mapOf(
            "politicianId" to politicianId,
            "lastUpdated" to LocalDateTime.now().toString(),
            "message" to "Statistics endpoint - direct repository calculation not implemented yet"
        )
        return Response.ok(stats).build()
    }
}
