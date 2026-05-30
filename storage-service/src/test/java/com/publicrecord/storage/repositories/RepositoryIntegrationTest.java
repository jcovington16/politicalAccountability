package com.publicrecord.storage.repositories;

import com.publicrecord.common.models.ContentItem;
import com.publicrecord.common.models.Politician;
import com.publicrecord.storage.config.DatabaseConfig;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Repository integration tests")
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("political_data_test")
            .withUsername("test")
            .withPassword("test");

    static DatabaseConfig databaseConfig;
    static PoliticianRepository politicianRepository;
    static ContentItemRepository contentItemRepository;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        databaseConfig = new DatabaseConfig(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword(),
                10
        );

        runLiquibaseMigrations();

        politicianRepository = new PoliticianRepository(databaseConfig);
        contentItemRepository = new ContentItemRepository(databaseConfig);
    }

    @Test
    @DisplayName("Should save and find politicians")
    void shouldSaveAndFindPoliticians() {
        UUID politicianId = UUID.randomUUID();
        Politician politician = new Politician(
                politicianId,
                "Jane",
                "Example",
                "Independent",
                "CO",
                "Representative",
                "Test biography",
                "https://example.test/jane.jpg",
                LocalDate.of(2024, 1, 1),
                null
        );

        assertThat(politicianRepository.save(politician)).isTrue();

        Politician found = politicianRepository.findById(politicianId);

        assertThat(found).isNotNull();
        assertThat(found.getFirstName()).isEqualTo("Jane");
        assertThat(found.getLastName()).isEqualTo("Example");
        assertThat(politicianRepository.searchByName("Exam", 10)).hasSize(1);
        assertThat(politicianRepository.findByState("CO", 10)).hasSize(1);
        assertThat(politicianRepository.findByParty("Independent", 10)).hasSize(1);
    }

    @Test
    @DisplayName("Should save and find content items for politicians")
    void shouldSaveAndFindContentItemsForPoliticians() {
        UUID politicianId = UUID.randomUUID();
        Politician politician = new Politician(
                politicianId,
                "Alex",
                "Content",
                "Democratic",
                "CA",
                "Senator",
                null,
                null,
                LocalDate.of(2023, 1, 3),
                null
        );
        assertThat(politicianRepository.save(politician)).isTrue();

        UUID contentId = UUID.randomUUID();
        LocalDateTime publishedAt = LocalDateTime.of(2025, 5, 1, 12, 30);
        ContentItem contentItem = new ContentItem(
                contentId,
                "Healthcare Town Hall",
                "article",
                "The senator discussed healthcare policy.",
                null,
                publishedAt,
                "hash-" + contentId,
                "https://example.test/article",
                politicianId,
                Collections.singletonList("healthcare"),
                Collections.singletonList("policy"),
                null,
                LocalDateTime.now()
        );

        assertThat(contentItemRepository.save(contentItem)).isTrue();

        ContentItem found = contentItemRepository.findById(contentId);

        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Healthcare Town Hall");
        assertThat(contentItemRepository.findByPoliticianId(politicianId, 10, 0)).hasSize(1);
        assertThat(contentItemRepository.findByContentType("article", 10)).hasSize(1);
        assertThat(contentItemRepository.searchByKeyword("healthcare", 10)).hasSize(1);
        assertThat(contentItemRepository.findByDateRange(
                publishedAt.minusDays(1),
                publishedAt.plusDays(1),
                10
        )).hasSize(1);
    }

    private static void runLiquibaseMigrations() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update();
        }
    }
}
