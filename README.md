# Political Accountability App

Public Record is a neutral political accountability platform for state and federal public officials. The goal is to help voters search a politician or bill and see public-record evidence: biography, offices, elections, bills, votes, public statements, claims, source citations, media/content, timeline activity, and import/audit history.

Core neutrality rule:

```text
We do not score politicians by ideology.
We score information by evidence quality.
```

The app should show what was said, voted on, sponsored, opposed, reported, cited, disputed, or verified, then let voters decide what matters to them.

## Current Status

This is an early-stage MVP with a real backend foundation and live-data-only public clients.

Implemented:

- Dropwizard API gateway.
- PostgreSQL schema with Liquibase migrations.
- React dashboard.
- Expo React Native mobile MVP.
- Local CSV/JSON ingestion.
- Congress.gov, GovInfo, Open States, Google Civic, GDELT, Guardian, RSS, and YouTube connector scaffolding.
- X and Bluesky public social-post discovery connector scaffolding.
- Normalized bill/source-citation ingestion.
- Politician profile aggregation endpoint.
- Politician timeline aggregation endpoint.
- Trust scoring model for evidence quality.
- Internal review/warning model for evidence safety, source quality, and publishability.
- Source registry and citations.
- Claims, fact checks, public statements, tags.
- Import batch visibility.
- Append-only audit log.
- Admin-token protection for internal endpoints.
- Public OpenAPI contract and smoke/load scripts for staging checks.

Still in progress:

- Full live data population.
- Open States vote/action depth beyond first people/bill pulls.
- Google Civic address/district matching UX.
- Auth roles beyond the local admin token.
- Public production deployment hardening.
- Expanding live profile completeness across offices, elections, votes, legislation, statements, media, and citations.

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
DATABASE_URL=jdbc:postgresql://localhost:5434/political_data
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
ADMIN_API_TOKEN=local-admin-token

CONGRESS_API_KEY=
GOVINFO_API_KEY=
OPENSTATES_API_KEY=
GOOGLE_CIVIC_API_KEY=
GOOGLE_CIVIC_ADDRESS=
GUARDIAN_API_KEY=
YOUTUBE_API_KEY=
X_BEARER_TOKEN=
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
Postgres:  localhost:5434 (container-to-container: postgres:5432)
Kafka:     localhost:29092 from host, kafka:9092 from containers
MinIO:     http://localhost:9000
```

The dashboard and mobile app never replace missing or unavailable live records with sample politicians or fabricated profile details. They show an explicit empty or unavailable state instead.

Kafka readiness and live-profile operations are documented in [docs/LIVE_DATA_OPERATIONS.md](docs/LIVE_DATA_OPERATIONS.md).

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

### Public Media Discovery

Fetch raw public media discovery records:

```sh
make ingest-media
```

Relevant `.env` values:

```sh
GDELT_QUERIES="Marco Rubio healthcare,Marco Rubio voting record"
GDELT_MAX_RECORDS=10
RSS_FEEDS="https://example.org/feed.xml|Example News,https://official.example.gov/news.xml|Official Press Room"
GUARDIAN_API_KEY=...
GUARDIAN_QUERIES="Marco Rubio healthcare,Marco Rubio voting record"
GUARDIAN_PAGE_SIZE=10
YOUTUBE_API_KEY=...
YOUTUBE_QUERIES="Marco Rubio hearing,Marco Rubio interview"
YOUTUBE_CHANNEL_IDS=
YOUTUBE_MAX_RESULTS=5
```

GDELT, Guardian, and RSS records are stored as `article` raw events. YouTube records are stored as `video` raw events. These are discovery/citation candidates, not verified claims. Downstream review should link them to politicians, bills, public statements, citations, and trust scores before the app presents them as evidence.

Public social discovery can also run through the media job:

```sh
BLUESKY_QUERIES="Marco Rubio"
X_BEARER_TOKEN=...
X_QUERIES="from:marcorubio"
make ingest-media
```

Social posts are stored as `social_post` raw events and should be normalized into `public_statements` only after the account is linked through `social_accounts` and `external_identifiers`. A post is evidence of what a public account said; it is not automatically proof that a claim inside the post is true.

## API Overview

The public OpenAPI contract is documented in `docs/openapi/public-record-api.yaml`.
See `docs/API_DOCUMENTATION.md` for the public/internal route boundary.

Public read/search endpoints:

```text
GET /politicians/{id}
GET /politicians/{id}/profile
GET /politicians/{id}/timeline
GET /search?query=...
GET /identity/politicians/resolve?query=...&state=...&party=...
GET /politicians/search/name?name=...
GET /politicians/state/{state}
GET /politicians/party/{party}

GET /bills/search?query=...
GET /bills/{id}
GET /bills/{id}/actions
GET /bills/{id}/citations
GET /claims?query=...
GET /politicians/{id}/claims
GET /citations?type=...&quality=...
GET /citations/{citationType}/{targetId}
GET /sources?type=...

GET /politicians/{politicianId}/votes
GET /bills/{billId}/votes

POST /trust/score
```

Internal/admin endpoints:

```text
GET /review/queue
GET /review/politicians/{id}/completeness
GET /review/metrics
POST /classification/civic
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

## Operator And Staging Checks

Import operators should follow `docs/IMPORT_OPERATOR_GUIDE.md` before loading official, civic, media, or social discovery records.

Useful checks:

```sh
scripts/api-smoke-test.sh
REQUESTS=30 QUERY="Trump" scripts/search-load-smoke.sh
scripts/release-candidate-check.sh
scripts/launch-monitor.sh
```

Staging performance guidance lives in `docs/STAGING_PERFORMANCE_RUNBOOK.md`.
Release and launch guidance lives in `docs/RELEASE_CHECKLIST.md`, `docs/API_FREEZE.md`, `docs/MVP_DATASET_FREEZE.md`, `docs/APP_STORE_PRIVACY_PREP.md`, and `docs/LAUNCH_MONITORING.md`.

## Politician Profile Aggregation

The main profile endpoint is:

```text
GET /politicians/{politicianId}/profile
GET /politicians/{politicianId}/timeline
```

The profile endpoint returns:

- politician record
- office history
- election history with same-seat candidates
- trust summary
- voting records
- voted bills
- bills supported
- bills opposed
- bills sponsored
- content items
- source citations
- timeline items

This is the endpoint the web and mobile profile screens should converge on as live data grows.

The timeline endpoint returns a chronological activity feed built from normalized PostgreSQL rows: votes, sponsored bills, bill actions, public statements, claims, content/media records, office history, and elections. Each event includes an evidence type, source context where available, publishability, and review warnings. This lets the app show a strong demo with live data while keeping claims, media, and official records clearly separated.

The identity endpoint helps ingestion and internal review match records from Congress.gov, Open States, FEC, Google Civic, media, YouTube, X, Bluesky, RSS, and other sources to the correct politician. Exact external IDs can auto-match. Weak name-only matches return `needsReview=true` so they do not become publishable evidence without confirmation.

Voting records are also available independently:

```text
GET /politicians/{politicianId}/votes
GET /bills/{billId}/votes
```

Those responses include the vote row plus joined bill or politician display fields so web/mobile screens do not need to make extra lookup requests for basic vote cards.

Bill detail and statement endpoints:

```text
GET /bills/{billId}
GET /statements?query=<text>
GET /politicians/{politicianId}/statements
```

`GET /bills/{billId}` returns a detail aggregate with sponsors, cosponsors, actions, citations, and votes. Public statements are modeled separately from news articles so direct quotes, speeches, interviews, press releases, hearings, debates, and social posts can be displayed with their own source and confidence context.

## Search Behavior

Current dashboard and mobile behavior:

1. Search stored PostgreSQL records through the API.
2. Display live results when they exist.
3. Show an explicit no-match or API-unavailable state without sample fallback.

Bill backend behavior:

1. Search PostgreSQL first.
2. If no local bill match and `CONGRESS_API_KEY` is set, fetch a small Congress.gov slice.
3. Store matching bills.
4. Search PostgreSQL again and return stored records.

Politician/backend ingestion should follow the same cache-first pattern:

1. Search PostgreSQL first.
2. On a controlled miss or explicit refresh, call official providers such as Congress.gov members, Open States, and Google Civic.
3. Upsert politicians, external identifiers, offices, bills, votes, citations, statements, claims, and content/media into PostgreSQL.
4. Return stored database records to the dashboard and mobile app.

This keeps external API usage low, protects provider keys on the server, and lets future searches reuse data that was already discovered.

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

## Review And Warnings

The public app should show records, citations, confidence context, and review warnings without deciding whether a politician or policy is good or bad. Voters decide what matters.

Internal review can flag evidence that needs caution before public display:

- missing or weak citations
- unresolved claims and allegations
- social posts from accounts that are not linked or verified
- prompt-injection-like scraped text
- potentially harmful, hateful, threatening, or discriminatory language that needs human review
- media records that are still citation candidates rather than verified facts

`POST /classification/civic` exists as protected internal review infrastructure only. It is not part of the public voter-facing API and should not be used to render public judgments such as "good", "bad", "problem-solving", or "problematic" labels. Public screens should prefer source context, citation counts, confidence, publish status, and neutral warnings.

## Privacy And Saved Data

The MVP has a public-record privacy guardrail that redacts obvious private contact details and blocks high-risk identifiers during local ingestion. The public app should not display home addresses, personal contact info, SSNs, payment data, private messages, or login-gated content.

Saved politicians are local-first. The dashboard stores saved snapshots in browser storage; mobile keeps the same local-first model and should only add backend sync after accounts, consent, delete/export controls, and privacy review exist.

See:

- `docs/PRIVACY_AND_PUBLIC_RECORD_POLICY.md`
- `docs/SAVED_POLITICIANS_STRATEGY.md`

## Presidents And Executive Officials

Congress.gov member ingestion covers Congress, and Open States covers state legislative data. Presidents are federal executive officials, so they do not appear from those legislative imports. Google Civic can return the President for an address-based representative lookup, but the database still needs a representative import or seed before the search bar can find presidential profiles.

For local/demo coverage, seed recent U.S. Presidents:

```sh
make ingest-federal-executives
```

This creates baseline searchable profiles for recent Presidents and citations to primary/official public sources. It does not replace richer executive-branch ingestion for executive orders, statements, appointments, vetoes, public schedules, and White House press materials.

## Rate Limits And Metrics

Public search endpoints have a lightweight in-memory rate limit. Configure with:

```sh
RATE_LIMIT_ENABLED=true
PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE=60
```

Protected metrics are available at:

```text
GET /review/metrics
X-Admin-Token: <ADMIN_API_TOKEN>
```

See `docs/API_METRICS_AND_ABUSE_PREVENTION.md` and `docs/STAGING_HARDENING_CHECKLIST.md`.

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
- external identifiers
- social accounts
- internal review records
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
