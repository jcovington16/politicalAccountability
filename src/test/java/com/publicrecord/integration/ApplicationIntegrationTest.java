package com.publicrecord.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Application Integration Tests")
class ApplicationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withDatabaseName("political_data_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.6.2")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"))
            .withEmbeddedZookeeper();

    @Container
    static GenericContainer<?> minio = new GenericContainer<>("minio/minio")
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    @BeforeEach
    void setUp() {
        // Set system properties for test containers
        System.setProperty("DATABASE_URL", postgres.getJdbcUrl());
        System.setProperty("ELASTICSEARCH_URL", "http://" + elasticsearch.getHost() + ":" + elasticsearch.getFirstMappedPort());
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", kafka.getBootstrapServers());
        System.setProperty("MINIO_ENDPOINT", "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
    }

    @Test
    @DisplayName("Should connect to PostgreSQL database")
    void shouldConnectToPostgreSQL() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(postgres.getJdbcUrl()).isNotEmpty();
    }

    @Test
    @DisplayName("Should connect to Elasticsearch")
    void shouldConnectToElasticsearch() {
        assertThat(elasticsearch.isRunning()).isTrue();
        assertThat(elasticsearch.getHttpHostAddress()).isNotEmpty();
    }

    @Test
    @DisplayName("Should connect to Kafka")
    void shouldConnectToKafka() {
        assertThat(kafka.isRunning()).isTrue();
        assertThat(kafka.getBootstrapServers()).isNotEmpty();
    }

    @Test
    @DisplayName("Should connect to MinIO")
    void shouldConnectToMinIO() {
        assertThat(minio.isRunning()).isTrue();
        assertThat(minio.getFirstMappedPort()).isNotNull();
    }

    @Test
    @DisplayName("Should have all required services running")
    void shouldHaveAllServicesRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(elasticsearch.isRunning()).isTrue();
        assertThat(kafka.isRunning()).isTrue();
        assertThat(minio.isRunning()).isTrue();
    }
}
