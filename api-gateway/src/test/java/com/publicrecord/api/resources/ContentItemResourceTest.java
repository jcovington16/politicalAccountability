package com.publicrecord.api.resources;

import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.ContentItemRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class ContentItemResourceTest {

    private final ContentItemResource resource = new ContentItemResource(
            new ContentItemRepository(dummyDatabaseConfig())
    );

    @Test
    void getContentItemRejectsInvalidUuid() {
        Response response = resource.getContentItem("not-a-uuid");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void searchContentRequiresKeyword() {
        Response response = resource.searchContent(
                "550e8400-e29b-41d4-a716-446655440000",
                "",
                50
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Keyword query parameter is required");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
