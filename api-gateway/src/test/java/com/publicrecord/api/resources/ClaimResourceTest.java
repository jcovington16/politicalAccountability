package com.publicrecord.api.resources;

import com.publicrecord.api.services.ClaimService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.ClaimRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class ClaimResourceTest {

    private final ClaimResource resource = new ClaimResource(
            new ClaimService(new ClaimRepository(dummyDatabaseConfig()))
    );

    @Test
    void searchClaimsRejectsInvalidType() {
        Response response = resource.searchClaims("vendor", "RUMOR", null, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("type must be one of");
    }

    @Test
    void politicianClaimsRejectsInvalidStatus() {
        Response response = resource.getPoliticianClaims(
                "550e8400-e29b-41d4-a716-446655440000",
                "ALLEGATION",
                "PUBLISHED",
                50
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("status must be one of");
    }

    @Test
    void politicianClaimsRejectsInvalidUuid() {
        Response response = resource.getPoliticianClaims("not-a-uuid", "ALLEGATION", null, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
