package com.publicrecord.api.resources

import org.slf4j.LoggerFactory
import com.publicrecord.storage.repositories.PoliticianRepository
import java.util.UUID
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/politicians")
@Produces(MediaType.APPLICATION_JSON)
class PoliticianResource(
    private val politicianRepository: PoliticianRepository
) {

    private val logger = LoggerFactory.getLogger(PoliticianResource::class.java)

    /**
     * Get politician by ID
     * Example: GET /politicians/550e8400-e29b-41d4-a716-446655440000
     */
    @GET
    @Path("/{id}")
    fun getPolitician(@PathParam("id") id: String): Response {
        logger.info("Fetching politician with ID: $id")

        return try {
            val politician = politicianRepository.findById(UUID.fromString(id))
            if (politician != null) {
                Response.ok(politician).build()
            } else {
                logger.warn("Politician not found: $id")
                Response.status(Response.Status.NOT_FOUND)
                    .entity("Politician not found").build()
            }
        } catch (e: IllegalArgumentException) {
            Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid UUID format").build()
        }
    }

    /**
     * Search politicians by name
     * Example: GET /politicians/search?name=Smith
     */
    @GET
    @Path("/search/name")
    fun searchByName(@QueryParam("name") nameQuery: String?): Response {
        if (nameQuery.isNullOrBlank()) {
            logger.error("Name parameter is missing")
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Name query parameter is required").build()
        }
        logger.info("Searching politicians with name: $nameQuery")
        val politicians = politicianRepository.searchByName(nameQuery)
        return Response.ok(politicians).build()
    }

    /**
     * Get politicians by state
     * Example: GET /politicians/state/CA
     */
    @GET
    @Path("/state/{state}")
    fun getPoliticiansByState(@PathParam("state") state: String): Response {
        logger.info("Fetching politicians from state: $state")
        val politicians = politicianRepository.findByState(state)
        return Response.ok(politicians).build()
    }

    /**
     * Get politicians by party
     * Example: GET /politicians/party/Democratic
     */
    @GET
    @Path("/party/{party}")
    fun getPoliticiansByParty(@PathParam("party") party: String): Response {
        logger.info("Fetching politicians from party: $party")
        val politicians = politicianRepository.findByParty(party)
        return Response.ok(politicians).build()
    }
}
