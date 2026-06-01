package com.publicrecord.api.resources;

import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.ImportRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class ImportResourceTest {

    private final ImportResource resource = new ImportResource(
            new ImportRepository(dummyDatabaseConfig())
    );

    @Test
    void getImportRejectsInvalidUuid() {
        Response response = resource.getImport("not-a-uuid");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void getImportsRejectsInvalidStatus() {
        Response response = resource.getImports("MAYBE", 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("status must be one of");
    }

    @Test
    void getImportRowsRejectsInvalidStatus() {
        Response response = resource.getImportRows("550e8400-e29b-41d4-a716-446655440000", "MAYBE", 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("status must be one of");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
