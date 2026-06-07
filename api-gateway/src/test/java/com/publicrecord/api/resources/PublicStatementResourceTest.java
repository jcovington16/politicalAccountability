package com.publicrecord.api.resources;

import com.publicrecord.api.services.PublicStatementService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.PublicStatementRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class PublicStatementResourceTest {

    private final PublicStatementResource resource = new PublicStatementResource(
            new PublicStatementService(new PublicStatementRepository(dummyDatabaseConfig()))
    );

    @Test
    void searchStatementsRejectsInvalidType() {
        Response response = resource.searchStatements("budget", "BLOG", 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("type must be one of");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
