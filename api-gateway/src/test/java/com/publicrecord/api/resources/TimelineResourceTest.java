package com.publicrecord.api.resources;

import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.ContentItemRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class TimelineResourceTest {

    private final TimelineResource resource = new TimelineResource(
            new ContentItemRepository(dummyDatabaseConfig())
    );

    @Test
    void getTimelineRejectsInvalidUuid() {
        Response response = resource.getTimeline("not-a-uuid", 50, 0, null);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void searchTimelineRequiresKeyword() {
        Response response = resource.searchTimeline(
                "550e8400-e29b-41d4-a716-446655440000",
                null,
                50
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Keyword query parameter is required");
    }

    @Test
    void getTimelineByDateRangeRequiresDates() {
        Response response = resource.getTimelineByDateRange(
                "550e8400-e29b-41d4-a716-446655440000",
                null,
                "2024-01-01T00:00:00",
                100
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("startDate and endDate parameters are required (ISO format)");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
