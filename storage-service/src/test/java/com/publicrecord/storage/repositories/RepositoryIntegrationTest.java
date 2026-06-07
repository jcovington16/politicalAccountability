package com.publicrecord.storage.repositories;

import com.publicrecord.common.models.ContentItem;
import com.publicrecord.common.models.AuditLogEntry;
import com.publicrecord.common.models.ImportBatch;
import com.publicrecord.common.models.ImportRowResult;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    static ImportRepository importRepository;
    static AuditLogRepository auditLogRepository;
    static ExternalIdentifierRepository externalIdentifierRepository;

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
        importRepository = new ImportRepository(databaseConfig);
        auditLogRepository = new AuditLogRepository(databaseConfig);
        externalIdentifierRepository = new ExternalIdentifierRepository(databaseConfig);
    }

    @Test
    @DisplayName("Should expose import batches, row results, and append-only audit logs")
    void shouldExposeImportBatchesRowsAndAppendOnlyAuditLogs() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO import_batches
                    (id, source_system, source_detail, status, completed_at, records_seen, records_imported, records_skipped, source_checksum, metadata)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?::jsonb)
                    """)) {
                stmt.setObject(1, batchId);
                stmt.setString(2, "test-importer");
                stmt.setString(3, "unit-test.csv");
                stmt.setString(4, "COMPLETED");
                stmt.setInt(5, 2);
                stmt.setInt(6, 1);
                stmt.setInt(7, 1);
                stmt.setString(8, "sha256:test");
                stmt.setString(9, "{\"file\":\"unit-test.csv\"}");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO import_row_results
                    (import_batch_id, source_record_id, target_type, target_id, status, message, row_payload)
                    VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                    """)) {
                stmt.setObject(1, batchId);
                stmt.setString(2, "row-1");
                stmt.setString(3, "BILL");
                stmt.setObject(4, targetId);
                stmt.setString(5, "IMPORTED");
                stmt.setString(6, "Imported test row");
                stmt.setString(7, "{\"billNumber\":\"HR-1\"}");
                stmt.executeUpdate();
            }
        }

        assertThat(auditLogRepository.append(
                "SYSTEM",
                "IMPORT_COMPLETED",
                "IMPORT_BATCH",
                null,
                batchId,
                "test-importer",
                batchId,
                "request-1",
                "{\"recordsSeen\":2}"
        )).isTrue();

        ImportBatch batch = importRepository.findBatch(batchId);
        assertThat(batch).isNotNull();
        assertThat(batch.getSourceSystem()).isEqualTo("test-importer");
        assertThat(batch.getSourceChecksum()).isEqualTo("sha256:test");
        assertThat(importRepository.findBatches("COMPLETED", 10)).extracting(ImportBatch::getId).contains(batchId);

        java.util.List<ImportRowResult> rows = importRepository.findRows(batchId, "IMPORTED", 10);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getSourceRecordId()).isEqualTo("row-1");

        java.util.List<AuditLogEntry> auditEntries = auditLogRepository.findRecent(10);
        assertThat(auditEntries).extracting(AuditLogEntry::getAction).contains("IMPORT_COMPLETED");

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            try (PreparedStatement stmt = connection.prepareStatement("UPDATE audit_log SET action = 'CHANGED' WHERE import_batch_id = ?")) {
                stmt.setObject(1, batchId);
                org.assertj.core.api.Assertions.assertThatThrownBy(stmt::executeUpdate)
                        .hasMessageContaining("audit_log is append-only");
            }
        }
    }

    @Test
    @DisplayName("Should upsert external identifiers and social accounts")
    void shouldUpsertExternalIdentifiersAndSocialAccounts() {
        UUID politicianId = UUID.randomUUID();
        assertThat(politicianRepository.save(new Politician(
                politicianId,
                "Jordan",
                "Social",
                "Independent",
                "CO",
                "Candidate",
                "Candidate biography",
                null,
                LocalDate.of(2026, 1, 1),
                null
        ))).isTrue();

        assertThat(externalIdentifierRepository.upsertExternalIdentifier(
                "POLITICIAN",
                politicianId,
                "FEC",
                "P60000001",
                "https://www.fec.gov/data/candidate/P60000001/",
                new java.math.BigDecimal("95.00"),
                "{\"candidate\":true}"
        )).isNotNull();

        assertThat(externalIdentifierRepository.findBySource("POLITICIAN", "FEC", "P60000001"))
                .isNotNull()
                .extracting("entityId")
                .isEqualTo(politicianId);

        assertThat(externalIdentifierRepository.upsertSocialAccount(
                politicianId,
                "BLUESKY",
                "@jordan.example",
                "https://bsky.app/profile/jordan.example",
                "Jordan Social",
                "SELF_ASSERTED",
                null,
                new java.math.BigDecimal("75.00"),
                "{\"source\":\"campaign site\"}"
        )).isNotNull();

        assertThat(externalIdentifierRepository.findSocialAccountsByPolitician(politicianId, 10))
                .hasSize(1)
                .first()
                .extracting("handle")
                .isEqualTo("jordan.example");
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

    @Test
    @DisplayName("Should create offices, elections, and same-seat candidate relationships")
    void shouldCreateOfficesElectionsAndCandidateRelationships() throws Exception {
        UUID officeId = UUID.randomUUID();
        UUID electionId = UUID.randomUUID();
        UUID firstCandidateId = UUID.randomUUID();
        UUID secondCandidateId = UUID.randomUUID();

        assertThat(politicianRepository.save(new Politician(
                firstCandidateId,
                "Casey",
                "Jordan",
                "Independent",
                "CO",
                "U.S. Representative",
                "Candidate biography",
                null,
                LocalDate.of(2026, 1, 1),
                null
        ))).isTrue();
        assertThat(politicianRepository.save(new Politician(
                secondCandidateId,
                "Taylor",
                "Morgan",
                "Republican",
                "CO",
                "U.S. Representative",
                "Candidate biography",
                null,
                LocalDate.of(2026, 1, 1),
                null
        ))).isTrue();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO offices
                    (id, name, branch, office_level, jurisdiction, state, district, seat_identifier, description)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, officeId);
                stmt.setString(2, "U.S. Representative Colorado District 3");
                stmt.setString(3, "LEGISLATIVE");
                stmt.setString(4, "FEDERAL");
                stmt.setString(5, "United States");
                stmt.setString(6, "CO");
                stmt.setString(7, "CO-03");
                stmt.setString(8, "FEDERAL:HOUSE:CO-03");
                stmt.setString(9, "Federal congressional district seat");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO politician_offices
                    (politician_id, office_id, title, start_date, is_current, source_url)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, firstCandidateId);
                stmt.setObject(2, officeId);
                stmt.setString(3, "U.S. Representative");
                stmt.setObject(4, LocalDate.of(2026, 1, 3));
                stmt.setBoolean(5, true);
                stmt.setString(6, "https://example.gov/offices/co-03");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO elections
                    (id, office_id, election_date, election_type, cycle_year, jurisdiction, source_url)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, electionId);
                stmt.setObject(2, officeId);
                stmt.setObject(3, LocalDate.of(2026, 11, 3));
                stmt.setString(4, "GENERAL");
                stmt.setInt(5, 2026);
                stmt.setString(6, "Colorado");
                stmt.setString(7, "https://example.gov/elections/co-03-2026");
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO election_candidates
                    (election_id, politician_id, party, ballot_status, result_status, source_url)
                    VALUES (?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, electionId);
                stmt.setObject(2, firstCandidateId);
                stmt.setString(3, "Independent");
                stmt.setString(4, "CERTIFIED");
                stmt.setString(5, "PENDING");
                stmt.setString(6, "https://example.gov/elections/co-03-2026");
                stmt.setObject(7, electionId);
                stmt.setObject(8, secondCandidateId);
                stmt.setString(9, "Republican");
                stmt.setString(10, "CERTIFIED");
                stmt.setString(11, "PENDING");
                stmt.setString(12, "https://example.gov/elections/co-03-2026");
                stmt.executeUpdate();
            }

            try (PreparedStatement candidates = connection.prepareStatement("""
                    SELECT p.first_name, p.last_name
                    FROM election_candidates ec
                    JOIN politicians p ON p.id = ec.politician_id
                    WHERE ec.election_id = ?
                    ORDER BY p.last_name
                    """)) {
                candidates.setObject(1, electionId);
                try (ResultSet rs = candidates.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("last_name")).isEqualTo("Jordan");
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("last_name")).isEqualTo("Morgan");
                    assertThat(rs.next()).isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("Should create citations, statements, claims, and fact checks")
    void shouldCreateCitationsStatementsClaimsAndFactChecks() throws Exception {
        UUID politicianId = UUID.randomUUID();
        assertThat(politicianRepository.save(new Politician(
                politicianId,
                "Riley",
                "Source",
                "Independent",
                "CO",
                "U.S. Representative",
                "Source testing biography",
                null,
                LocalDate.of(2024, 1, 1),
                null
        ))).isTrue();

        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        )) {
            UUID sourceId = UUID.randomUUID();
            UUID citationId = UUID.randomUUID();
            UUID statementId = UUID.randomUUID();
            UUID claimId = UUID.randomUUID();

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO source_registry (id, name, source_type, homepage_url, reputation_score)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, sourceId);
                stmt.setString(2, "Congressional Record");
                stmt.setString(3, "OFFICIAL_RECORD");
                stmt.setString(4, "https://www.congress.gov/congressional-record");
                stmt.setBigDecimal(5, new java.math.BigDecimal("99.00"));
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO source_citations
                    (id, source_id, citation_type, target_id, title, url, source_quality, confidence)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, citationId);
                stmt.setObject(2, sourceId);
                stmt.setString(3, "STATEMENT");
                stmt.setObject(4, statementId);
                stmt.setString(5, "Floor statement transcript");
                stmt.setString(6, "https://example.gov/record/statement");
                stmt.setString(7, "OFFICIAL_RECORD");
                stmt.setBigDecimal(8, new java.math.BigDecimal("98.00"));
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO public_statements
                    (id, politician_id, statement_type, title, quote, venue, statement_date, source_citation_id, confidence)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, statementId);
                stmt.setObject(2, politicianId);
                stmt.setString(3, "SPEECH");
                stmt.setString(4, "Floor statement on transparency");
                stmt.setString(5, "Public records should be easy to inspect.");
                stmt.setString(6, "House floor");
                stmt.setObject(7, LocalDateTime.of(2026, 4, 17, 12, 0));
                stmt.setObject(8, citationId);
                stmt.setBigDecimal(9, new java.math.BigDecimal("95.00"));
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO claims
                    (id, politician_id, statement_id, claim_text, claim_type, status, confidence, first_seen_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, claimId);
                stmt.setObject(2, politicianId);
                stmt.setObject(3, statementId);
                stmt.setString(4, "The politician supported public records access.");
                stmt.setString(5, "VERIFIED_FACT");
                stmt.setString(6, "VERIFIED");
                stmt.setBigDecimal(7, new java.math.BigDecimal("95.00"));
                stmt.setObject(8, LocalDateTime.of(2026, 4, 17, 12, 0));
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO fact_checks (claim_id, rating, summary, checked_by, source_citation_id)
                    VALUES (?, ?, ?, ?, ?)
                    """)) {
                stmt.setObject(1, claimId);
                stmt.setString(2, "TRUE");
                stmt.setString(3, "Verified against official transcript.");
                stmt.setString(4, "system");
                stmt.setObject(5, citationId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = connection.prepareStatement("""
                    SELECT c.claim_text, fc.rating
                    FROM claims c
                    JOIN fact_checks fc ON fc.claim_id = c.id
                    WHERE c.id = ?
                    """)) {
                stmt.setObject(1, claimId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("rating")).isEqualTo("TRUE");
                    assertThat(rs.getString("claim_text")).contains("public records");
                }
            }
        }
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
