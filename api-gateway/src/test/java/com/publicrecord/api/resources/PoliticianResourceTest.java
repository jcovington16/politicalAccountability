package com.publicrecord.api.resources;

import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.PoliticianRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticianResourceTest {

    private final PoliticianResource resource = new PoliticianResource(
            new PoliticianRepository(dummyDatabaseConfig())
    );

    @Test
    void getPoliticianRejectsInvalidUuid() {
        Response response = resource.getPolitician("not-a-uuid");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void searchByNameRequiresNameQuery() {
        Response response = resource.searchByName(" ");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Name query parameter is required");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
