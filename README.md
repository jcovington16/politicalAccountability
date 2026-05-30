# Political Accountability App

## 📌 Overview
The Political Accountability App is designed to **track and store data** about politicians, their voting history, proposed bills, news articles, and media coverage. The goal is to **provide transparency** by aggregating data from various sources and presenting it in a structured manner.

## 🏗️ Architecture
The long-term direction is a **microservices architecture**, but the current running application is intentionally simpler while the product and data model are still early-stage.

### Current Running Architecture
At the moment, `api-gateway` is the only HTTP application. It uses the `storage-service` Gradle module as an in-process persistence library:

```text
Client
  -> api-gateway
     -> storage-service repositories/classes, in process
        -> PostgreSQL / Elasticsearch / MinIO / Kafka
```

This means `storage-service` is **sidelined as a standalone HTTP service** for now. The module is still kept because it contains useful storage code and represents the future microservice boundary, but the app does not currently run or call a separate `storage-service:8082` server.

Why this is the current choice:
- The app is early-stage, and the domain model is still changing.
- Direct repository wiring is easier to read, debug, and test right now.
- It avoids an imaginary network dependency on a storage HTTP service that has not been implemented yet.
- The future microservice boundary remains visible in the module structure, so it can be extracted later.

When the app is ready to return to the microservice route, `storage-service` should get its own server entry point, REST endpoints, health checks, Docker Compose service, auth/service-to-service rules, and contract tests. At that point, `api-gateway` can switch back to a real HTTP client boundary.

### **Backend Stack**
- **Java/Kotlin (No Spring Boot)** –  Core backend services using Dropwizard for the API Gateway
- **Gradle (Kotlin DSL)** – Build tool
- **PostgreSQL** – Structured data storage (politicians, voting records, bills)
- **Elasticsearch** – Full-text search for news articles and social media content.
- **MinIO (S3-Compatible)** – Media storage for videos, audio, and images.
- **Kafka** – Event-driven architecture for data ingestion and processing
- **Redis** – Caching frequently accessed data

### **Microservices Structure**
| Module | Purpose |
|--------|---------|
| `api-gateway` | Exposes REST endpoints using Dropwizard; currently wires repositories directly. |
| `ingestion-service` | Scrapes data from news sources, government APIs, and social media |
| `processing-service` | Analyzes data, applies NLP for fact-checking, and enriches records |
| `storage-service` | Currently an in-process storage library. Later, this is the intended standalone storage microservice. |
| `event-streaming` | Processes Kafka events for real-time updates. |
| `common` | Contains shared models, utilities, and configuration classes. |

## Database Migrations
Database schema changes are managed with Liquibase Community.

The changelog lives in:

```text
storage-service/src/main/resources/db/changelog/db.changelog-master.xml
storage-service/src/main/resources/db/changelog/changes/
```

Use the Makefile targets instead of running raw Liquibase commands:

```sh
make db-status
make db-migrate
make db-validate
make db-history
make db-rollback
make db-new name=add_content_indexes
make db-tag tag=before_large_change
```

Local configuration is read from environment variables or a local `.env` file. Start from:

```sh
cp .env.example .env
```

Do not commit `.env` or real database credentials. The committed defaults are only for local Docker development.

Liquibase is used as a developer/deployment tool, not as application boot logic. That keeps migration credentials out of the API runtime and makes schema changes intentional.

## Event Processing
The ingestion and processing services exchange content with a shared Jackson-backed event contract:

```text
ingestion-service -> Kafka raw-content -> processing-service -> Kafka processed-content -> processed content sink -> PostgreSQL
```

The processed content sink currently persists processed `ContentItem` events to PostgreSQL through `ContentItemRepository`. Elasticsearch indexing and MinIO media writes should be added to that sink once those contracts are concrete.

## Configuration And Security Notes
Local development keeps Docker-friendly defaults, but production should provide secrets and endpoints through environment-specific configuration:

```sh
APP_ENV=production
DATABASE_URL=jdbc:postgresql://...
DATABASE_USER=...
DATABASE_PASSWORD=...
KAFKA_BOOTSTRAP_SERVERS=...
ELASTICSEARCH_URL=...
MINIO_ENDPOINT=...
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
```

When `APP_ENV=production`, startup validation requires database and MinIO secrets and rejects wildcard CORS origins. CORS is wired in the Dropwizard servlet layer and defaults to local frontend origins only.

Kafka and Elasticsearch are still local-development posture in `docker-compose.yml`. Before production use, enable service authentication/TLS or private-network-only access for Kafka, Elasticsearch, PostgreSQL, and MinIO.

## Local File Ingestion
The local ingestion pipeline imports CSV or JSON files into PostgreSQL and indexes searchable fields into Elasticsearch/OpenSearch.

Run migrations first:

```sh
make db-migrate
```

Then import a directory:

```sh
make ingest-local dir=data/ingestion
```

The importer looks for these files:

```text
politicians.csv or politicians.json
bills.csv or bills.json
votes.csv or votes.json
news_articles.csv, news_articles.json, news.csv, or news.json
```

Recommended columns:

```text
politicians: id, first_name, last_name, full_name, party, state, office, biography, profile_image_url, start_date, end_date
bills: id, bill_number, title, description, introduced_by, status, introduced_date, last_action_date, bill_url
votes: id, politician_id, bill_id, vote_type, vote_date
news_articles: id, politician_id, title, source, published_date, url, content
```

Validation is intentionally strict for identifiers and required fields. Invalid rows are skipped and logged; valid rows are upserted. Search indexing failures are logged but do not roll back PostgreSQL imports, because PostgreSQL is the source of truth.

The current schema is **not yet complete** for the full political-accountability domain. It has politicians, bills, votes, media files, content items, provenance, and news articles. Offices, elections, source citations, public-statement-specific records, fact checks, richer tags, and audit history still need dedicated migrations before that model should be considered production-complete.

## Trust Scoring
Political information is classified before it is treated as equally reliable. The current trust scoring model separates:

```text
VERIFIED_FACT
DIRECT_QUOTE
VOTING_RECORD
ALLEGATION
OPINION_PIECE
UNRESOLVED_CLAIM
```

Each score includes:

```text
sourceQuality
citationCount
recencyDays
confidenceLevel
score
explanation
```

Source quality values are:

```text
OFFICIAL_RECORD
PRIMARY_SOURCE
REPUTABLE_NEWS
ADVOCACY_OR_PARTISAN
SOCIAL_MEDIA
UNKNOWN
```

The API exposes a scoring helper:

```http
POST /trust/score
Content-Type: application/json

{
  "informationType": "VOTING_RECORD",
  "sourceQuality": "OFFICIAL_RECORD",
  "citationCount": 4,
  "publishedDate": "2026-05-29"
}
```

## 🔹 Database Schema
### **Politicians Table**
```sql
CREATE TABLE politicians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    party VARCHAR(50),
    state VARCHAR(50),
    office VARCHAR(100),
    start_date DATE,
    end_date DATE NULL,
    profile_image_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### **Voting Records Table**
```sql
CREATE TABLE voting_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE CASCADE,
    bill_id UUID REFERENCES bills(id),
    vote_type VARCHAR(20) CHECK (vote_type IN ('YEA', 'NAY', 'ABSTAIN')),
    vote_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### **News Articles Table**
```sql
CREATE TABLE news_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    politician_id UUID REFERENCES politicians(id) ON DELETE SET NULL,
    title TEXT NOT NULL,
    source VARCHAR(255) NOT NULL,
    published_date TIMESTAMP,
    url TEXT UNIQUE NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## 🚀 Running the Application
### **1️⃣ Set Up PostgreSQL**
```sh
docker run --name postgres -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=secret -p 5432:5432 -d postgres
```

### **2️⃣ Set Up MinIO for Media Storage**
```sh
docker run -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ROOT_USER=admin" -e "MINIO_ROOT_PASSWORD=secretpass" \
  quay.io/minio/minio server /data --console-address ":9001"
```

### **3️⃣ Run Kafka for Event Streaming**
```sh
docker-compose up -d kafka zookeeper
```

### **4️⃣ Build and Run Services**
```sh
./gradlew clean build
./gradlew :api-gateway:run
```

## 📡 API Endpoints
### **Get Politician Details**
```http
GET /api/politicians/{id}
```
Response:
```json
{
  "id": "1234-5678",
  "full_name": "John Doe",
  "party": "Independent",
  "state": "NY",
  "office": "Senator",
  "start_date": "2018-01-01",
  "end_date": null
}
```

### **Search News Articles**
```http
GET /api/news?query=tax+reform
```

### **Search Bills**
```http
GET /bills/search?query=healthcare&status=Pending
```

### **Get Bill Details**
```http
GET /bills/{id}
```

### **Get Politician Voting Records**
```http
GET /politicians/{politicianId}/votes
```

### **Get Votes For A Bill**
```http
GET /bills/{billId}/votes
```

## React Dashboard
The React dashboard lives in `dashboard/`.

```sh
make dashboard-install
make dashboard-dev
```

The dashboard runs on:

```text
http://localhost:5173
```

It proxies `/api/*` to the Dropwizard API on `http://localhost:8080`. If the API is not running, the dashboard falls back to sample data so the interface remains usable during frontend work.

Primary dashboard areas:

```text
Politician search
Biography and profile summary
Voting record
Bills supported/opposed
Public statements
Controversies and unresolved claims
Accomplishments
Source citations
Timeline of activity
Security and integrity controls
```

The Security tab tracks controls for data integrity, misinformation risk, source manipulation, prompt injection, scraping risks, privacy, authorization, audit logs, and abuse prevention.

## React Native Mobile MVP
The voter mobile MVP lives in `mobile/` and uses Expo so the same codebase can run on iOS, Android, and web preview.

```sh
make mobile-install
make mobile-start
make mobile-typecheck
```

Implemented MVP screens:

```text
Search
Politician profile
Voting record
Issue stance
Timeline
Compare two politicians
Saved politicians
Source citations
```

The app currently uses local sample data that mirrors the React dashboard. The next implementation step is wiring these screens to the Dropwizard API endpoints and production API configuration.

App store packaging is configured through `mobile/app.json` and `mobile/eas.json`. Publishing still requires Apple Developer and Google Play accounts, production icons/screenshots, a privacy policy URL, and completed store data-safety forms.

## 30-Day Build Plan
The detailed 30-day implementation plan is tracked in `docs/30_DAY_BUILD_PLAN.md`.

## 👥 Contributors
- **Joshua Covington** – Lead Developer

## 📜 License
This project is licensed under the **MIT License**.
