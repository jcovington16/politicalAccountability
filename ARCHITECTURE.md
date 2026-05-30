## Architecture Implementation Guide

This document provides a comprehensive guide to the restructured Political Accountability Application based on the proposed modern microservices architecture.

### Overview

The application follows a microservices architecture with the following key components:

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (8080)                      │
│  ┌──────────────┬──────────────┬──────────────────────────┐  │
│  │ Politicians  │ Timeline     │ Content Items            │  │
│  │ Resources    │ Resources    │ Resources                │  │
│  └──────────────┴──────────────┴──────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐
  │ PostgreSQL   │  │ Elasticsearch│  │  Kafka Broker   │
  │ (Politicians │  │  (Content    │  │ (Events)        │
  │ Content)     │  │  Search)     │  └─────────────────┘
  └──────────────┘  └──────────────┘         ▲
                                              │
                    ┌─────────────────────────┼─────────────────┐
                    │                         │                 │
                    ▼                         ▼                 ▼
            ┌───────────────────┐  ┌──────────────────┐ ┌────────────────┐
            │ Ingestion Service │  │Processing Service│ │ Storage Service│
            │ - RSS Feeds       │  │ - NER Enrichment │ │ - Repositories │
            │ - Twitter API     │  │ - Entity Linking │ │ - Services     │
            │ - Web Scraping    │  │ - Sentiment      │ │ - Databases    │
            │ - YouTube         │  │ - Language Det.  │ │ - Elasticsearch│
            └───────────────────┘  └──────────────────┘ └────────────────┘
                    │                         │
                    └─────────┬───────────────┘
                              │
                    Kafka Topics: raw-content
                                 processed-content
```

---

## 1. Folder Structure

The project is organized into the following modules:

```
political-accountability-app/
├── common/                          # Shared models and utilities
│   ├── models/
│   │   ├── Politician.kt
│   │   ├── ContentItem.kt
│   │   ├── NewsArticle.kt
│   │   ├── Bill.kt
│   │   └── MediaFile.kt
│   ├── interfaces/
│   │   └── DatabaseInterface.kt
│   └── build.gradle.kts
│
├── api-gateway/                     # REST API (Dropwizard)
│   ├── resources/
│   │   ├── PoliticianResource.kt
│   │   ├── TimelineResource.kt
│   │   ├── ContentItemResource.kt
│   │   ├── NewsResource.kt
│   │   ├── BillResource.kt
│   │   └── MediaResource.kt
│   ├── App.kt
│   ├── AppConfig.kt
│   ├── CorsConfig.kt
│   ├── StorageServiceClient.kt
│   └── build.gradle.kts
│
├── ingestion-service/               # Data Collection
│   ├── IngestionService.kt
│   ├── connectors/
│   │   └── ExampleConnectors.kt (RSS, Twitter, YouTube, News Scraper)
│   ├── Main.java
│   └── build.gradle.kts
│
├── processing-service/              # Data Enrichment
│   ├── ProcessingService.kt
│   ├── enrichers/
│   │   └── ExampleEnrichers.kt (NER, Entity Linking, Language Detection, Sentiment)
│   ├── Main.java
│   └── build.gradle.kts
│
├── storage-service/                 # Persistence Layer
│   ├── config/
│   │   ├── DatabaseConfig.kt       # PostgreSQL schema init
│   │   └── ElasticsearchConfig.kt  # ES index init
│   ├── services/
│   │   ├── DatabaseService.kt
│   │   ├── ElasticSearchService.kt
│   │   ├── KafkaProducerService.kt
│   │   ├── KafkaConsumerService.kt
│   │   ├── MinIOService.kt
│   │   └── KafkaService.kt
│   ├── repositories/
│   │   ├── PoliticianRepository.kt
│   │   ├── ContentItemRepository.kt
│   │   ├── NewsRepository.kt
│   │   ├── BillRepository.kt
│   │   └── MediaRepository.kt
│   ├── managed/
│   │   ├── DatabaseManagedService.kt
│   │   ├── ElasticSearchManagedService.kt
│   │   ├── MinIOManagedService.kt
│   │   └── KafkaManagedService.kt
│   └── build.gradle.kts
│
├── event-streaming/                 # Kafka setup
│   └── build.gradle.kts
│
├── docker-compose.yml               # Infrastructure (PostgreSQL, ES, Kafka, MinIO)
├── Dockerfile                       # API Gateway container
├── build.gradle.kts                 # Root build configuration
└── settings.gradle.kts              # Module definitions
```

---

## 2. Database Schema (PostgreSQL)

The application uses the following PostgreSQL schema:

### Politicians Table
```sql
CREATE TABLE politicians (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    party VARCHAR(100),
    state VARCHAR(50),
    office VARCHAR(100),
    biography TEXT,
    profile_image_url TEXT,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Content Items Table
```sql
CREATE TABLE content_items (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,  -- 'tweet', 'article', 'video', 'speech'
    text_body TEXT,
    media_url TEXT,
    published_at TIMESTAMP NOT NULL,
    content_hash VARCHAR(256) UNIQUE,
    source_url TEXT NOT NULL,
    politician_id UUID NOT NULL,
    keywords TEXT[],
    tags TEXT[],
    indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (politician_id) REFERENCES politicians (id) ON DELETE CASCADE
);
```

### Provenance Table
```sql
CREATE TABLE provenance (
    id UUID PRIMARY KEY,
    content_item_id UUID NOT NULL,
    source_type VARCHAR(100),          -- 'twitter_api', 'scraper', 'rss', etc.
    extractor_version VARCHAR(50),
    confidence FLOAT,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE
);
```

---

## 3. Elasticsearch Index

The application indexes content in Elasticsearch with the following mapping:

```json
{
  "index": "political-content",
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "politicianId": { "type": "keyword" },
      "contentType": { "type": "keyword" },
      "title": { "type": "text", "analyzer": "english" },
      "textBody": { "type": "text", "analyzer": "english" },
      "publishedAt": { "type": "date" },
      "keywords": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "tags": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "sourceUrl": { "type": "keyword" },
      "contentHash": { "type": "keyword" },
      "indexedAt": { "type": "date" },
      "provenance": {
        "type": "object",
        "properties": {
          "sourceType": { "type": "keyword" },
          "timestamp": { "type": "date" },
          "extractorVersion": { "type": "keyword" },
          "confidence": { "type": "float" }
        }
      }
    }
  }
}
```

---

## 4. Kafka Topics

The application uses two main Kafka topics for the event pipeline:

### `raw-content`
- **Purpose**: Raw, unenriched content from ingestion sources
- **Producer**: `IngestionService`
- **Consumer**: `ProcessingService`
- **Message Format**: JSON with `id`, `title`, `contentType`, `textBody`, `source`, `politicianName`, etc.

### `processed-content`
- **Purpose**: Enriched content with NLP metadata, entity links, and sentiment
- **Producer**: `ProcessingService`
- **Consumer**: Storage Service (writes to PostgreSQL and Elasticsearch)
- **Message Format**: JSON with all fields from raw-content plus enriched fields

---

## 5. Service Responsibilities

### Ingestion Service
**Handles**: Data collection from external sources
- RSS Feeds (news feeds, politician press releases)
- Twitter API (tweets, updates)
- YouTube API (video content)
- Web Scrapers (news sites, official pages)

**Output**: Publishes raw content to `raw-content` Kafka topic

**Key Classes**:
- `IngestionService`: Orchestrator
- `ContentConnector`: Interface for connectors
- `RSSFeedConnector`, `TwitterConnector`, `YouTubeConnector`, `NewsScraperConnector`: Implementations

### Processing Service
**Handles**: Data enrichment using NLP
- Named Entity Recognition (NER) - extracts keywords and entities
- Politician Linking - links content to specific politicians
- Language Detection - detects language
- Sentiment Analysis - analyzes content sentiment

**Input**: Consumes from `raw-content` Kafka topic
**Output**: Publishes enriched content to `processed-content` Kafka topic

**Key Classes**:
- `ProcessingService`: Orchestrator
- `ContentEnricher`: Interface for enrichers
- `NEREnricher`, `PoliticianLinkerEnricher`, `LanguageDetectionEnricher`, `SentimentAnalysisEnricher`: Implementations

### Storage Service
**Handles**: Data persistence and retrieval
- PostgreSQL: Politicians, content items, provenance metadata
- Elasticsearch: Full-text search on content
- MinIO: Media file storage
- Repositories: Data access layer

**Key Classes**:
- `DatabaseConfig`: Schema initialization
- `ElasticsearchConfig`: Index initialization
- `PoliticianRepository`, `ContentItemRepository`, `NewsRepository`, `BillRepository`, `MediaRepository`
- `KafkaProducerService`, `KafkaConsumerService`

### API Gateway
**Handles**: REST API endpoints for client applications
- `/politicians/{id}` - Get politician details
- `/politicians/search/name?name={name}` - Search politicians
- `/politicians/state/{state}` - Get politicians by state
- `/politicians/party/{party}` - Get politicians by party
- `/politicians/{id}/timeline` - Get politician's content timeline
- `/politicians/{id}/timeline/filter?contentType={type}` - Filter by content type
- `/politicians/{id}/timeline/search?keyword={keyword}` - Search timeline
- `/politicians/{id}/timeline/daterange?startDate={date}&endDate={date}` - Date range search
- `/politicians/{id}/timeline/stats` - Get timeline statistics
- `/content/{id}` - Get content item
- `/news/search?keyword={query}` - Search news

---

## 6. Data Flow

### Ingestion Pipeline
```
1. Ingestion Service starts periodic collection
2. Connectors fetch data from various sources
3. Raw content published to Kafka [raw-content topic]
4. Processing Service consumes messages
5. Enrichers enhance content with NLP
6. Processed content published to Kafka [processed-content topic]
7. Storage Service saves to PostgreSQL and Elasticsearch
8. API Gateway exposes data via REST endpoints
```

### Query Flow
```
1. Client calls API Gateway endpoint
2. API Gateway routes to appropriate Resource
3. Resource calls StorageServiceClient or directly accesses repositories
4. Storage Service queries PostgreSQL, Elasticsearch, or MinIO
5. Results marshaled to JSON and returned to client
```

---

## 7. Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21
- Gradle

### Building the Project

```bash
# Clone the repository
cd political-accountability-app

# Build all modules
./gradlew clean build

# Build shadowJar for API Gateway
./gradlew shadowJar
```

### Starting Infrastructure

```bash
# Start all services (PostgreSQL, Elasticsearch, Kafka, MinIO, API Gateway)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Starting Individual Services

```bash
# Start Ingestion Service
cd ingestion-service
java -jar build/libs/ingestion-service-1.0-SNAPSHOT.jar

# Start Processing Service
cd processing-service
java -jar build/libs/processing-service-1.0-SNAPSHOT.jar

# API Gateway is started automatically via docker-compose
```

---

## 8. API Examples

### Get Politician
```bash
curl -X GET "http://localhost:8080/politicians/550e8400-e29b-41d4-a716-446655440000"
```

### Search Politicians by Name
```bash
curl -X GET "http://localhost:8080/politicians/search/name?name=Smith"
```

### Get Politician Timeline
```bash
curl -X GET "http://localhost:8080/politicians/550e8400-e29b-41d4-a716-446655440000/timeline?limit=50&offset=0"
```

### Search Timeline by Keyword
```bash
curl -X GET "http://localhost:8080/politicians/550e8400-e29b-41d4-a716-446655440000/timeline/search?keyword=healthcare"
```

### Filter Timeline by Content Type
```bash
curl -X GET "http://localhost:8080/politicians/550e8400-e29b-41d4-a716-446655440000/timeline/filter?contentType=tweet"
```

### Get Timeline Statistics
```bash
curl -X GET "http://localhost:8080/politicians/550e8400-e29b-41d4-a716-446655440000/timeline/stats"
```

---

## 9. Models

### Politician
```kotlin
data class Politician(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val party: String,
    val state: String,
    val office: String,
    val biography: String?,
    val profileImageUrl: String?,
    val startDate: LocalDate,
    val endDate: LocalDate?
)
```

### ContentItem
```kotlin
data class ContentItem(
    val id: UUID,
    val title: String,
    val contentType: String,
    val textBody: String?,
    val mediaUrl: String?,
    val publishedAt: LocalDateTime,
    val contentHash: String,
    val sourceUrl: String,
    val politicianId: UUID,
    val keywords: List<String>,
    val tags: List<String>,
    val provenance: ProvenanceMetadata?,
    val indexedAt: LocalDateTime
)
```

### ProvenanceMetadata
```kotlin
data class ProvenanceMetadata(
    val sourceType: String,
    val timestamp: LocalDateTime,
    val extractorVersion: String?,
    val confidence: Float?
)
```

---

## 10. Integration Points

### Adding a New Content Source
1. Create a new class implementing `ContentConnector` in `ingestion-service/connectors/`
2. Implement the `fetch()` method to return `List<RawContentItem>`
3. Register the connector in `IngestionService.registerConnector()`

### Adding a New Content Enricher
1. Create a new class implementing `ContentEnricher` in `processing-service/enrichers/`
2. Implement the `enrich()` method to enhance `RawContentData`
3. Register the enricher in `ProcessingService.registerEnricher()`

### Adding a New Repository
1. Create a new class in `storage-service/repositories/` extending database access patterns
2. Use `DatabaseConfig` for connections
3. Register in appropriate service layer

---

## 11. Environment Variables

The application respects the following environment variables:

```bash
DATABASE_URL=jdbc:postgresql://postgres:5432/political_data
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
ELASTICSEARCH_URL=http://elasticsearch:9200
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

---

## 12. Monitoring and Logs

### Docker Compose Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
docker-compose logs -f kafka

# Last 100 lines
docker-compose logs -f --tail 100 api-gateway
```

### Application Logs
Logs are written to:
- API Gateway: Standard output + `logs/api-gateway.log`
- All services: Standard output + Logback configuration

---

## 13. Known Issues and TODOs

### TODOs
- [ ] Implement actual Twitter API connector (currently mocked)
- [ ] Implement actual YouTube API connector (currently mocked)
- [ ] Integrate real NLP library (Stanford Core NLP configured but not fully integrated)
- [ ] Add authentication/authorization to API endpoints
- [ ] Add rate limiting to API
- [ ] Implement database migrations with Flyway or Liquibase
- [ ] Add unit and integration tests for repositories
- [ ] Add API documentation with Swagger/OpenAPI
- [ ] Improve error handling and validation
- [ ] Add metrics and monitoring (Prometheus, Grafana)
- [ ] Add distributed tracing (Jaeger)

### Known Issues
- Provisioning/data initialization needs implementation
- Some enrichers need actual NLP library integration
- Database schema initialization runs on every startup (consider migrations)

---

## 14. Configuration Files

### config.yml (API Gateway)
```yaml
server:
  port: 8080
  adminPort: 8081
  rootPath: /api/*

logging:
  level: INFO
  appenders:
    - type: file
      currentLogFilename: logs/api-gateway.log
      maxFileSize: 100MB
      retainedFileCount: 10
```

---

## 15. Performance Considerations

- **PostgreSQL**: Use indexes on `politician_id`, `published_at`, `content_hash`
- **Elasticsearch**: Optimize shard count based on data volume
- **Kafka**: Configure appropriate partition count and replication factor
- **API Gateway**: Use connection pooling (configured in Dropwizard)
- **Caching**: Implement Redis caching layer for frequently accessed content

---

## 16. Security Considerations

- Implement authentication (OAuth2, JWT)
- Validate and sanitize all user inputs
- Use HTTPS for all API communications
- Secure environment variables and secrets
- Implement audit logging for sensitive operations
- Add rate limiting and DDoS protection
- Regular security scanning of dependencies

---

## 17. Deployment

### Docker Deployment
```bash
# Build and push image
docker build -t yourregistry/political-app:latest .
docker push yourregistry/political-app:latest

# Deploy with docker-compose
docker-compose -f docker-compose.yml up -d
```

### Kubernetes Deployment
Create Kubernetes manifests for:
- API Gateway deployment
- Ingestion Service deployment
- Processing Service deployment
- Storage Service deployment
- ConfigMaps for configuration
- Secrets for credentials
- Services for service discovery
- PersistentVolumes for data

---

This implementation provides a solid foundation for the political accountability platform. Next steps would be to integrate actual data sources, enhance the NLP pipeline, add comprehensive error handling, and implement the missing features listed in the TODOs.

