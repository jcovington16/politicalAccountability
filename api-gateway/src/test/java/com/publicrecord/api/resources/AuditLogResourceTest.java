package com.publicrecord.api.resources;

import com.publicrecord.storage.config.DatabaseConfig;
import com.publicrecord.storage.repositories.AuditLogRepository;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogResourceTest {

    @Test
    void getAuditLogReturnsListEvenWhenRepositoryFailsClosed() {
        AuditLogResource resource = new AuditLogResource(
                new AuditLogRepository(new DatabaseConfig("jdbc:postgresql://localhost:1/unused", "unused", "unused", 1))
        );

        Response response = resource.getAuditLog(50);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(List.class);
    }
}
