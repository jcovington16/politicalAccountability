package com.publicrecord.api.resources;

import com.publicrecord.api.services.VotingRecordService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.VotingRecordRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class VotingRecordResourceTest {

    private final VotingRecordResource resource = new VotingRecordResource(
            new VotingRecordService(new VotingRecordRepository(dummyDatabaseConfig()))
    );

    @Test
    void getVotesByPoliticianRejectsInvalidUuid() {
        Response response = resource.getVotesByPolitician("not-a-uuid", 100);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void getVotesByBillRejectsInvalidUuid() {
        Response response = resource.getVotesByBill("not-a-uuid", 100);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
