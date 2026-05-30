package com.publicrecord.api.resources;

import com.publicrecord.api.services.BillService;
import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.BillRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

class BillResourceTest {

    private final BillResource resource = new BillResource(
            new BillService(new BillRepository(dummyDatabaseConfig()))
    );

    @Test
    void getBillRejectsInvalidUuid() {
        Response response = resource.getBill("not-a-uuid");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity()).isEqualTo("Invalid UUID format");
    }

    @Test
    void searchBillsRejectsInvalidStatus() {
        Response response = resource.searchBills("health", "Maybe", 50);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getEntity().toString()).contains("status must be one of");
    }

    private static DatabaseConfig dummyDatabaseConfig() {
        return new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1);
    }
}
