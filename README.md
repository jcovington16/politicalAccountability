# Political Accountability App

Public Record is a neutral political accountability platform for state and federal public officials. The goal is to help voters search a politician or bill and see public-record evidence: biography, offices, elections, bills, votes, public statements, claims, source citations, media/content, timeline activity, and import/audit history.

Core neutrality rule:

```text
We do not score politicians by ideology.
We score information by evidence quality.
```

The app should show what was said, voted on, sponsored, opposed, reported, cited, disputed, or verified, then let voters decide what matters to them.

## Current Status

This is an early-stage MVP with a real backend foundation and sample-rich frontend fallback.

Implemented:

- Dropwizard API gateway.
- PostgreSQL schema with Liquibase migrations.
- React dashboard.
- Expo React Native mobile MVP.
- Local CSV/JSON ingestion.
- Congress.gov, GovInfo, Open States, and Google Civic connector scaffolding.
- Normalized bill/source-citation ingestion.
- Politician profile aggregation endpoint.
- Trust scoring model for evidence quality.
- Source registry and citations.
- Claims, fact checks, public statements, tags.
- Import batch visibility.
- Append-only audit log.
- Admin-token protection for internal endpoints.

Still in progress:

- Full live data population.
- Open States vote/action depth beyond first people/bill pulls.
- Google Civic address/district matching UX.
- Auth roles beyond the local admin token.
- Public production deployment hardening.
- Replacing dashboard/mobile sample fallback with full live API mode.

## Architecture

The long-term direction is microservices, but the current running system is intentionally simpler:

```text
React dashboard / mobile app
  -> api-gateway
     -> storage-service repositories, in process
        -> PostgreSQL
        -> Elasticsearch/OpenSearch
        -> Kafka
        -> MinIO

ingestion-service
  -> official APIs / local files
  -> PostgreSQL
  -> Elasticsearch/OpenSearch
  -> Kafka raw-content when requested
```

`storage-service` is currently **not** a standalone HTTP service. It is kept as an in-process persistence module because the app is still early-stage and the domain model is changing. When the product stabilizes, this module can become a real storage microservice with its own server, auth, health checks, Docker service, and contract tests.

## Modules

| Module | Purpose |
| --- | --- |
| `api-gateway` | Dropwizard HTTP API. Public search/read endpoints plus protected internal import/audit endpoints. |
| `storage-service` | Repository layer, storage configs, managed service wrappers, Liquibase changelog resources. |
| `ingestion-service` | Local file imports and official API connectors. |
| `processing-service` | Placeholder for enrichment/NLP/fact processing workflows. |
| `event-streaming` | Kafka/event-streaming support. |
| `common` | Shared models, trust scoring, JSON/event utilities. |
| `dashboard` | React web dashboard. |
| `mobile` | Expo React Native voter MVP. |

## Local Environment

The local `.env` file is ignored by Git.

Create or update it from:

```sh
cp .env.example .env
```

Important local values:

```sh
DATABASE_URL=jdbc:postgresql://localhost:5432/political_data
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
ADMIN_API_TOKEN=local-admin-token

CONGRESS_API_KEY=
GOVINFO_API_KEY=
OPENSTATES_API_KEY=
GOOGLE_CIVIC_API_KEY=
GOOGLE_CIVIC_ADDRESS=
```

Paste real API keys into `.env` locally only. Do not commit `.env`.

## Running Locally

Start infrastructure:

```sh
make dev
```

Apply database migrations:

```sh
make db-migrate
```

Run the API:

```sh
make run
```

Run the dashboard:

```sh
make dashboard-install
make dashboard-dev
```

Run the mobile app:

```sh
make mobile-install
make mobile-start
```

Useful URLs:

```text
Dashboard: http://localhost:5173 or the next available Vite port
API:       http://localhost:8080
Admin:     http://localhost:8081
Postgres:  localhost:5432
Kafka:     localhost:29092 from host, kafka:9092 from containers
MinIO:     http://localhost:9000
```

If the dashboard API is unavailable or the database has no matching live data, it falls back to sample data so UI work can continue.

## Data Ingestion

### Local CSV/JSON

Import local files:

```sh
make ingest-dry-run
make ingest-local dir=data/ingestion
```

`make ingest-dry-run` validates the sample CSV/JSON templates without writing to PostgreSQL. It checks required fields, UUID/date formats, vote values, and politician/bill references so bad files are caught before import.

Supported file names:

```text
politicians.csv / politicians.json
bills.csv / bills.json
votes.csv / votes.json
news_articles.csv / news_articles.json
```

Templates live in `data/templates/`.

### Congress.gov And GovInfo

Fetch raw events:

```sh
make ingest-congress-bills
make ingest-govinfo-packages
```

Fetch Congress.gov member profiles:

```sh
make ingest-congress-members
```

For a state-focused federal seed, set for example:

```sh
CONGRESS_MEMBER_STATE=FL
CONGRESS_CURRENT_MEMBER=false
CONGRESS_MEMBER_LIMIT=250
```

Normalize official federal data into PostgreSQL:

```sh
make ingest-official-normalized
```

This writes bills, bill actions, source registry entries, source citations, import batches, row results, and audit events.

### Open States And Google Civic

Fetch state/civic data:

```sh
make ingest-state-civic
```

Relevant `.env` values:

```sh
OPENSTATES_API_KEY=...
OPENSTATES_JURISDICTION=ocd-jurisdiction/country:us/state:co/government
OPENSTATES_SESSION=
OPENSTATES_LIMIT=25

GOOGLE_CIVIC_API_KEY=...
GOOGLE_CIVIC_ADDRESS="Denver, CO"
```

Open States currently seeds state politicians and bills. Google Civic seeds address-based representative context. Imported records get `external_identifiers` so future pulls update existing records instead of duplicating them.

## API Overview

Public read/search endpoints:

```text
GET /politicians/{id}
GET /politicians/{id}/profile
GET /politicians/search/name?name=...
GET /politicians/state/{state}
GET /politicians/party/{party}

GET /bills/search?query=...
GET /bills/{id}
GET /bills/{id}/actions
GET /bills/{id}/citations

GET /politicians/{politicianId}/votes
GET /bills/{billId}/votes

POST /trust/score
```

Internal/admin endpoints:

```text
GET /imports
GET /imports?status=COMPLETED
GET /imports/{id}
GET /imports/{id}/rows
GET /imports/{id}/rows?status=FAILED
GET /audit-log
```

Admin endpoints require:

```text
X-Admin-Token: <ADMIN_API_TOKEN>
```

Local default:

```text
X-Admin-Token: local-admin-token
```

Use a long random `ADMIN_API_TOKEN` outside local development.

## Politician Profile Aggregation

The main profile endpoint is:

```text
GET /politicians/{politicianId}/profile
```

It returns:

- politician record
- voting records
- voted bills
- bills supported
- bills opposed
- bills sponsored
- content items
- source citations
- timeline items

This is the endpoint the web and mobile profile screens should converge on as live data grows.

## Search Behavior

Current dashboard behavior:

1. Try the real API.
2. If the API is down or returns no matches, show matching sample fallback data.

Bill backend behavior:

1. Search PostgreSQL first.
2. If no local bill match and `CONGRESS_API_KEY` is set, fetch a small Congress.gov slice.
3. Store matching bills.
4. Search PostgreSQL again and return stored records.

Politician backend search is currently database-backed. Open States and Google Civic ingestion are the next source of real politician records.

## Trust Scoring

The app scores information quality, not ideology.

Information types:

```text
VERIFIED_FACT
DIRECT_QUOTE
VOTING_RECORD
ALLEGATION
OPINION_PIECE
UNRESOLVED_CLAIM
```

Source qualities:

```text
OFFICIAL_RECORD
PRIMARY_SOURCE
REPUTABLE_NEWS
ADVOCACY_OR_PARTISAN
SOCIAL_MEDIA
UNKNOWN
```

Example:

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

## Database

Migrations are managed by Liquibase.

```sh
make db-status
make db-migrate
make db-validate
make db-history
make db-rollback
make db-new name=add_short_description
```

Major schema areas:

- politicians
- offices
- politician offices
- elections
- election candidates
- bills
- bill actions
- voting records
- media files
- content items
- news articles
- source registry
- source citations
- public statements
- claims
- fact checks
- tags/taggings
- import batches
- import row results
- audit log
- external identifiers

## Audit And Import Visibility

Every official normalized import creates an import batch and row result records. The audit log is append-only at the database layer using triggers that block update/delete.

Use import visibility to answer:

- Which import ran?
- Which source did it use?
- How many records were seen/imported/skipped?
- Which rows failed?
- What source identifiers map to internal records?

## Frontend Apps

### Dashboard

The dashboard is a voter/admin MVP that supports:

- politician search
- bill search
- politician profile tabs
- voting records
- bills supported/opposed
- statements
- controversies
- accomplishments
- citations
- timeline
- internal security/integrity view

Run:

```sh
make dashboard-dev
```

### Mobile

The mobile MVP uses Expo and supports:

- search
- bill search
- politician profile
- voting record
- issue stance
- timeline
- compare politicians
- saved politicians
- source citations
- bill detail

Run:

```sh
make mobile-start
```

App store packaging is configured through `mobile/app.json` and `mobile/eas.json`, but production release still needs store accounts, screenshots, privacy policy, icons, data-safety forms, and production API config.

## Security Notes

- `.env` is ignored and must not be committed.
- External API keys are server-side only.
- Admin import/audit endpoints require `X-Admin-Token`.
- Production must not use `local-admin-token`.
- Production must not use wildcard CORS.
- Kafka, Elasticsearch, PostgreSQL, and MinIO need private networking or auth/TLS before production exposure.
- The Security dashboard tab is internal/admin only and should not be part of the public voter experience.
- The current Week 1 threat model is documented in `docs/THREAT_MODEL.md`.

## Verification Commands

```sh
./gradlew test
./gradlew :storage-service:integrationTest
DATABASE_URL=offline:postgresql ./scripts/db/liquibase.sh validate
make ingest-dry-run
cd dashboard && npm run build
cd mobile && npm run typecheck
```

## Build Plan

The detailed 30-day implementation plan is tracked in:

```text
docs/30_DAY_BUILD_PLAN.md
```

## Contributors

- Joshua Covington

## License

MIT
