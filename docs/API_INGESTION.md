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
7. GDELT, Guardian, RSS, and YouTube for public media discovery only.
8. NewsAPI/Event Registry or licensed feeds later if production coverage requires paid reliability.

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
GUARDIAN_API_KEY=...
YOUTUBE_API_KEY=...
X_BEARER_TOKEN=...
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
GET /bills/{billId}/votes
GET /statements?query=budget
GET /politicians/{politicianId}/statements
GET /claims?query=budget&type=ALLEGATION&status=UNRESOLVED
GET /politicians/{politicianId}/claims
GET /politicians/{politicianId}/timeline
GET /search?query=budget
GET /identity/politicians/resolve?query=Marco%20Rubio&state=FL
GET /citations?type=CLAIM&quality=REPUTABLE_NEWS
GET /citations/{citationType}/{targetId}
GET /sources?type=OFFICIAL_RECORD
```

`GET /bills/{billId}` returns a detail aggregate with the bill row, sponsors, cosponsors, actions, citations, and joined vote records. Sponsor/cosponsor data is stored in `bill_sponsors` so the app can distinguish who introduced a bill from who later joined it.

Public statements are separate from generic articles/content. They support direct quotes, speeches, interviews, press releases, debate/hearing excerpts, social posts, source citations, confidence, and a `suspiciousContent` flag for scraped text that looks like prompt injection or manipulation instructions.

Claims are separate from public statements and articles. Use `claim_type` to distinguish verified facts, direct quotes, voting records, allegations, opinion pieces, and unresolved claims. Public claim DTOs include a trust score, fact checks, citation count, `publishable`, and review warnings. Claims without a category or citation support should not be treated as publishable evidence.

Source citation endpoints expose reusable citations and source registry entries. Citation DTOs include manipulation warnings for issues such as unknown source quality or non-HTTPS URLs.

The politician timeline endpoint is an aggregate view built from normalized PostgreSQL records. It combines votes, sponsored bills, bill actions, public statements, claims, content/media items, offices, and elections into one chronological feed. The feed does not turn media headlines into facts; each item includes an evidence type, source context where available, publishability, and review warnings.

Internal review endpoints are protected by `X-Admin-Token`. They help reviewers find missing citations, unresolved claims, suspicious scraped text, and incomplete profiles. They should not drive public labels that judge whether a politician or action is good or bad.

## Search Backfill Behavior

The app uses a cache-first search model:

1. Search PostgreSQL first.
2. If bill search has no local matches and `CONGRESS_API_KEY` is configured, fetch a small official slice from Congress.gov.
3. Store matching bills, actions, citations, and import/audit records in PostgreSQL.
4. Re-run the database search and return stored records to the web/mobile app.

This keeps PostgreSQL as the source of truth while still letting the app become more useful as voters search. Politician search is currently database-backed; external politician backfill should use Open States and Google Civic next because names alone are not reliable enough for jurisdiction matching.

The same pattern should be used for live politician and media lookups:

- Query local `politicians`, `external_identifiers`, `bills`, `voting_records`, `public_statements`, `claims`, `content_items`, and `source_citations` first.
- On a controlled miss or explicit refresh, call the relevant official/media provider.
- Upsert normalized rows with stable external identifiers and source citations.
- Return the stored database records to the app.
- Keep provider rate limits low and never expose provider API keys to web or mobile clients.

## Identity Resolution

The identity graph is built around `external_identifiers` and `social_accounts`.

Use:

```sh
GET /identity/politicians/resolve?query=Marco%20Rubio&state=FL&party=Republican&office=Senate
GET /identity/politicians/resolve?sourceSystem=CONGRESS_GOV&externalId=R000595
```

The response returns candidate politicians, confidence, match reasons, known external IDs, known social accounts, and a `needsReview` flag. Exact external identifiers can safely match one internal politician. Name-only or close-scored matches should stay internal until a reviewer or source-specific importer confirms the identity.

Candidate profiles for people who have not held office should come from candidate/election sources such as FEC, state election feeds, Google Civic election data, Open States where available, official campaign sites, and controlled CSV/JSON imports. These records may have campaign statements, finance filings, endorsements, debates, and media coverage before they have votes or sponsored bills.

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

## Public Media Discovery

Media discovery helps the app find public coverage, public statements, interviews, hearings, debates, town halls, and videos that may be relevant to a politician or bill. These sources are not treated as official truth by default. They create raw citation candidates that later processing must review, classify, dedupe, and link.

Run:

```sh
make ingest-media
```

Optional `.env` values:

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

BLUESKY_QUERIES="Marco Rubio"
BLUESKY_LIMIT=25

X_BEARER_TOKEN=...
X_QUERIES="from:marcorubio"
X_MAX_RESULTS=10

NEWS_SCRAPER_ENABLED=false
NEWS_SCRAPER_URLS="https://example.org/politics/article|Example News"
NEWS_SCRAPER_MAX_PAGES=10
```

Connector behavior:

- `GdeltDocConnector` calls the free GDELT DOC API and emits `contentType = article`, `source = GDELT`, `sourceQuality = MEDIA_AGGREGATOR`.
- `GuardianOpenPlatformConnector` calls The Guardian Open Platform and emits `contentType = article`, `source = The Guardian`, `sourceQuality = PUBLIC_MEDIA`.
- `RSSFeedConnector` parses RSS/Atom feeds and emits `contentType = article`, `sourceQuality = PUBLIC_MEDIA`.
- `YouTubeDataConnector` calls the official YouTube Data API and emits `contentType = video`, `source = YouTube`, `sourceQuality = PUBLIC_MEDIA`.
- `BlueskyPostConnector` calls Bluesky's public AppView search endpoint and emits `contentType = social_post`, `source = Bluesky`, `sourceQuality = SOCIAL_MEDIA`.
- `XPostConnector` calls the official X API with a bearer token and emits `contentType = social_post`, `source = X`, `sourceQuality = SOCIAL_MEDIA`.
- `NewsScraperConnector` fetches explicit allowlisted public article URLs and emits `contentType = article`, `sourceQuality = PUBLIC_MEDIA`.

HTML scraping policy:

- Prefer official APIs, RSS feeds, and licensed sources before scraping.
- Only scrape explicit public article URLs from legitimate sources that are appropriate for public citation.
- Do not scrape paywalled, login-gated, private, or sensitive pages.
- Do not bypass technical restrictions, access controls, CAPTCHAs, or anti-bot systems.
- Respect robots.txt and keep page limits low.
- Store scraped records as citation candidates, not verified facts.
- Keep the original URL, source name, and scraper policy metadata.

How YouTube fits the mission:

- Official committee/hearing channels can show what politicians said during public proceedings.
- Official politician channels can show their own public statements.
- Interviews, debates, podcasts, and town halls can become source citations for direct quotes after transcript review.
- The connector stores video metadata and URLs only. Transcript fetching and quote extraction should be a separate reviewed pipeline so the app does not overstate what a video proves.

How social posts fit the mission:

- Public posts can show what a politician, campaign, office, or verified public account said in their own words.
- Social accounts must be linked through `social_accounts` and source citations before posts are attached to a politician.
- Social posts should normalize into `public_statements` only after identity review; claims inside posts should become separate `claims` records with their own evidence status.
- Deleted or edited posts should be handled with archive URLs where lawful and available, plus retrieval timestamps.
- Do not scrape private, locked, deleted, login-gated, or bypassed content.

Security and neutrality rules:

- Do not scrape private or gated media.
- Do not use HTML scraping for bulk crawling or paywall bypass.
- Do not treat a headline, video title, or article summary as verified fact.
- Preserve the original URL and source metadata.
- Link media records to claims/statements only after citation and human review.
- Flag prompt-injection-like scraped text before any AI processing.

## Politician Profile Aggregation

The API exposes a profile aggregation endpoint for the web and mobile politician pages:

```sh
GET /politicians/{politicianId}/profile
GET /politicians/{politicianId}/timeline
```

The profile response groups the politician record with office history, election history, trust summary, joined voting records, bills supported/opposed, sponsored bills, content items, citations, and timeline items. The timeline response focuses only on chronological activity with category counts, publishable counts, review-required counts, and source/review metadata. Open States and Google Civic ingestion should feed these endpoints by adding authoritative politicians, offices, bills, votes, source citations, and election context to PostgreSQL.

Voting records can also be fetched directly:

```sh
GET /politicians/{politicianId}/votes
GET /bills/{billId}/votes
```

These responses include joined display fields such as `billNumber`, `billTitle`, `politicianName`, party, state, and source URL context where available.

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
- GDELT, Guardian, RSS, YouTube, and news/article APIs are discovery feeds, not ground-truth sources.
- Keep `GDELT_MAX_RECORDS` and `YOUTUBE_MAX_RESULTS` small while developing. YouTube quota is finite even when the API is free to start.

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
