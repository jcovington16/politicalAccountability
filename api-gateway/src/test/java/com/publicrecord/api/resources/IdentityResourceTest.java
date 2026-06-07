package com.publicrecord.api.resources;

import com.publicrecord.api.services.IdentityResolutionService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.ExternalIdentifierRepository;
import com.publicrecord.storage.repositories.PoliticianRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityResourceTest {
    private final DatabaseConfig databaseConfig = new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    private final IdentityResource resource = new IdentityResource(
            new IdentityResolutionService(
                    new PoliticianRepository(databaseConfig),
                    new ExternalIdentifierRepository(databaseConfig)
            )
    );

    @Test
    void resolvePoliticianRequiresQueryOrExternalIdentifier() {
        Response response = resource.resolvePolitician(null, null, null, null, null, null, 10);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Provide either query or sourceSystem plus externalId");
    }
}
