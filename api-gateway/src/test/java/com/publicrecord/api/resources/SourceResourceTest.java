package com.publicrecord.api.resources;

import com.publicrecord.api.services.SourceService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.SourceCitationRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class SourceResourceTest {

    private final SourceResource resource = new SourceResource(
            new SourceService(new SourceCitationRepository(dummyDatabaseConfig()))
    );

    @Test
    void citationsRejectInvalidType() {
        Response response = resource.searchCitations("BLOG", null, null, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("type must be one of");
    }

    @Test
    void citationsRejectInvalidQuality() {
        Response response = resource.searchCitations("CLAIM", "UNREVIEWED", null, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("quality must be one of");
    }

    @Test
    void targetCitationsRejectInvalidUuid() {
        Response response = resource.getTargetCitations("CLAIM", "not-a-uuid", 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void sourcesRejectInvalidSourceType() {
        Response response = resource.getSources("BLOG", null, 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("type must be one of");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
