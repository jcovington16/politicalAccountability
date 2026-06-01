# API Ingestion

External API keys must stay server-side. Web and mobile clients call the Public Record API only; ingestion jobs use provider APIs and store normalized records in PostgreSQL.

## Source Order

Implement sources one at a time:

1. Congress.gov for federal bill metadata and actions.
2. GovInfo for bill text, Congressional Record, public laws, and Federal Register documents.
3. Open States for state legislators, bills, votes, and actions.
4. FEC for federal candidates, committees, receipts, and disbursements.
5. Google Civic for address-based representative and election lookup.
6. CourtListener for judicial records and opinions.
7. NewsAPI/GDELT for article discovery only.

## Secrets

Use local `.env` or a secrets manager:

```sh
CONGRESS_API_KEY=...
GOVINFO_API_KEY=...
OPENSTATES_API_KEY=...
FEC_API_KEY=...
GOOGLE_CIVIC_API_KEY=...
COURTLISTENER_API_KEY=...
NEWSAPI_API_KEY=...
```

Never commit real key values. Rotate development keys before production.

## Congress.gov Bills

The first connector is `CongressGovBillConnector`.

Run once without Kafka:

```sh
CONGRESS_API_KEY=... make ingest-congress-bills
```

Run with Kafka publishing:

```sh
CONGRESS_API_KEY=... KAFKA_BOOTSTRAP_SERVERS=localhost:29092 make ingest-congress-bills
```

Optional controls:

```sh
CONGRESS_NUMBER=119
CONGRESS_BILL_TYPE=hr
CONGRESS_BILL_LIMIT=20
```

The connector emits raw content events with:

- `contentType = bill`
- `source = Congress.gov`
- `sourceQuality = OFFICIAL_RECORD`
- Congress number, bill type, bill number, chamber, update date, latest action date, and latest action text in metadata.

Downstream processors should map these events into normalized bill, source citation, action, sponsor, and search-index records.

## Congress.gov Members

Congress.gov member profiles can seed federal politician search:

```sh
make ingest-congress-members
```

Optional controls:

```sh
CONGRESS_MEMBER_STATE=FL
CONGRESS_CURRENT_MEMBER=false
CONGRESS_MEMBER_LIMIT=250
```

This importer writes `politicians`, `source_citations`, and `external_identifiers` rows using Congress.gov BioGuide IDs as the stable external identifier.

## GovInfo Packages

GovInfo provides official package summaries and content links for congressional bills, bill status, Congressional Record, public laws, Federal Register, and other government documents.

Run once without Kafka:

```sh
GOVINFO_API_KEY=... make ingest-govinfo-packages
```

Run with Kafka publishing:

```sh
GOVINFO_API_KEY=... KAFKA_BOOTSTRAP_SERVERS=localhost:29092 make ingest-govinfo-packages
```

Optional controls:

```sh
GOVINFO_COLLECTION=BILLS
GOVINFO_START_DATE_TIME=2026-01-01T00:00:00Z
GOVINFO_PAGE_SIZE=10
```

Useful collections:

- `BILLS`: official bill text packages.
- `BILLSTATUS`: bill status metadata packages.
- `CREC`: Congressional Record.
- `PLAW`: public laws.
- `FR`: Federal Register.

The connector emits raw content events with:

- `contentType = official_document`
- `source = GovInfo`
- `sourceQuality = OFFICIAL_RECORD`
- Package ID, collection, branch, document type, congress/session, issue dates, and content links in metadata.

Downstream processors should attach GovInfo events as official citations, bill text, Congressional Record entries, public laws, or Federal Register records depending on `collectionCode`.

## Normalize Official Data Into PostgreSQL

Use this command when you want fetched official API data to become usable app data immediately:

```sh
make ingest-official-normalized
```

The command reads `.env`, fetches from any configured official source, and writes:

- `bills`: normalized bill records keyed by bill number.
- `bill_actions`: latest official bill actions.
- `source_registry`: trusted source metadata.
- `source_citations`: official citations attached to bills.
- `import_batches` and `import_row_results`: audit records for every import run and row outcome.

Start with small limits while developing:

```sh
CONGRESS_BILL_LIMIT=10 GOVINFO_PAGE_SIZE=5 make ingest-official-normalized
```

After importing, the API exposes the normalized data through:

```sh
GET /bills/search?query=budget
GET /bills/{billId}
GET /bills/{billId}/actions
GET /bills/{billId}/citations
```

## Search Backfill Behavior

The app uses a cache-first search model:

1. Search PostgreSQL first.
2. If bill search has no local matches and `CONGRESS_API_KEY` is configured, fetch a small official slice from Congress.gov.
3. Store matching bills in PostgreSQL.
4. Re-run the database search and return stored records to the web/mobile app.

This keeps PostgreSQL as the source of truth while still letting the app become more useful as voters search. Politician search is currently database-backed; external politician backfill should use Open States and Google Civic next because names alone are not reliable enough for jurisdiction matching.

## State And Civic Ingestion

Open States API v3 is used for state legislative people and bills. Its root URL is `https://v3.openstates.org/`, API keys are required, and keys can be sent in the `X-API-KEY` header. Google Civic is used for address-based representative context; Google requires an API key for requests.

Run:

```sh
make ingest-state-civic
```

Required/optional `.env` values:

```sh
OPENSTATES_API_KEY=...
OPENSTATES_JURISDICTION=ocd-jurisdiction/country:us/state:co/government
OPENSTATES_LIMIT=25
GOOGLE_CIVIC_API_KEY=...
GOOGLE_CIVIC_ADDRESS="Denver, CO"
```

The importer upserts politicians, bills, source citations, and `external_identifiers` records. Those external identifiers are the dedupe bridge that lets future Open States/Google Civic pulls update the same internal records instead of creating duplicates.

## Politician Profile Aggregation

The API exposes a profile aggregation endpoint for the web and mobile politician pages:

```sh
GET /politicians/{politicianId}/profile
```

The response groups the politician record with voting records, bills supported/opposed, sponsored bills, content items, citations, and timeline items. Open States and Google Civic ingestion should feed this endpoint by adding authoritative politicians, offices, bills, votes, source citations, and election context to PostgreSQL.

## Import Visibility

Import runs are visible through API endpoints:

```sh
GET /imports
GET /imports?status=COMPLETED
GET /imports/{importBatchId}
GET /imports/{importBatchId}/rows
GET /imports/{importBatchId}/rows?status=FAILED
GET /audit-log
```

`import_batches` shows run-level status and counts. `import_row_results` shows per-row imported/skipped/failed outcomes and messages. `audit_log` is append-only and records system import completion events plus future admin/source/trust changes.

These endpoints are internal/admin-only. Requests must include:

```sh
X-Admin-Token: <ADMIN_API_TOKEN>
```

Local development defaults to `local-admin-token`; shared and production environments must provide a long random `ADMIN_API_TOKEN`.

## Rate Limits

- Congress.gov supports high hourly volume, but keep local pulls bounded with `CONGRESS_BILL_LIMIT`.
- Open States GraphQL should be treated as `1 request/sec` and `500/day`.
- News/article APIs are discovery feeds, not ground-truth sources.

## Local Kafka Verification

Local Docker exposes Kafka for host tools on `localhost:29092`; containers use `kafka:9092`.

Start infrastructure:

```sh
make dev
```

Watch raw ingestion events:

```sh
make kafka-raw-log
```

Publish Congress.gov or GovInfo events:

```sh
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 make ingest-congress-bills
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 make ingest-govinfo-packages
```

The raw logger is intentionally a verification tool. Final persistence should normalize events into the domain tables instead of forcing every official document into `content_items`.
