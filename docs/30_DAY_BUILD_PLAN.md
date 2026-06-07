# 30-Day Build Plan

Goal: ship a credible web and mobile MVP that helps voters understand politicians through source-backed public records, and holds politicians accountable by connecting what they say, sponsor, vote for or against, and do in public life.

Core product promise:

```text
Educate voters.
Hold politicians accountable.
Stay neutral.
Show evidence.
Let people decide.
```

The app should never rank politicians by ideology or popularity. It should organize public facts, direct quotes, voting records, bills, claims, media coverage, citations, and timelines so voters can see a clear track record.

## Week 1: Foundation And Data Model

### Day 1 - Complete
- Backend: finalized schema gaps for offices, elections, public statements, source citations, fact checks, tags, audit logs, and import batches in `docs/DAY_1_FOUNDATION.md`.
- Frontend: confirmed dashboard navigation and data contract needs.
- Mobile: confirmed MVP screen list and app-store bundle identifiers in `mobile/README.md`, `mobile/app.json`, and `mobile/eas.json`.
- Data ingestion: defined canonical CSV/JSON templates in `data/templates/`.
- Testing: added Liquibase changelog validation to CI.
- Security: defined roles for public reader, analyst, admin, and data-import operator.
- Deployment: documented local dev boot sequence.

### Day 2 - Complete
- Backend: added Liquibase migrations for `offices`, `elections`, `politician_offices`, and `election_candidates`.
- Frontend: confirmed the product flow should be search-first, then politician-specific tabs for overview, votes, bills, articles, issues, timeline, compare, and citations.
- Mobile: added an API client contract shell and reshaped the MVP navigation around search/saved entry points plus profile tabs.
- Data ingestion: documented politician identity matching rules in `docs/IDENTITY_MATCHING.md`.
- Testing: added repository integration coverage for office/election same-seat candidate relationships.
- Security: documented source provenance as a publishing requirement.
- Deployment: added environment variable inventory in `docs/ENVIRONMENT.md`.

### Day 3 - Complete
- Backend: added `source_registry` and `source_citations` tables.
- Frontend: kept citations reusable across profile, votes, bills, statements, controversies, and bill detail views.
- Mobile: source citations screen already matches the future endpoint shape.
- Data ingestion: API connector docs now require official source names, URLs, and source quality metadata.
- Testing: Liquibase validation covers the new citation schema.
- Security: source quality enum and source-provenance requirement are now represented in schema.
- Deployment: local Kafka and API-ingestion runbook documented in `docs/API_INGESTION.md`.

### Day 4 - Complete
- Backend: added `public_statements`, `claims`, `fact_checks`, `tags`, and `taggings` tables.
- Frontend: statements remain separated from generic content in the dashboard.
- Mobile: issue stance model exists and is ready for backend wiring.
- Data ingestion: statement/claim/fact-check records have dedicated schema targets.
- Testing: Liquibase validation covers statement, claim, fact-check, and tag schema.
- Security: official documents and raw ingestion events are kept separate from final normalized claims.
- Deployment: Kafka raw-content logger added for local verification before normalized persistence.

### Day 5 - Complete
- Backend: bill actions, source citations, import batches, row-level import results, and append-only `audit_log` are in schema/API flow.
- Frontend: internal Security view now reflects audit-log progress and remaining auth requirement.
- Mobile: keep audit internal; no voter-facing exposure yet.
- Data ingestion: official API normalization logs import batch metadata and row outcomes.
- Testing: added API validation tests and DB-backed import/audit integration coverage.
- Security: audit records are append-only at the database layer; auth remains required before public/shared admin exposure.
- Deployment: import and audit endpoints documented in `docs/API_INGESTION.md`.

### Day 6 - Complete
- Backend: `import_batches` and `import_row_results` are available through import visibility endpoints.
- Frontend: import status remains internal/admin scope; public voter app stays focused on search/profile/bills.
- Mobile: no mobile work unless week slips.
- Data ingestion: normalizer reports skipped rows with reasons for unsupported or failed source events.
- Testing: integration test covers one full import batch plus row lookup.
- Security: `source_checksum` column added for reproducible imports.
- Deployment: import visibility and audit endpoints documented.

### Day 7 - Complete
- Backend: refactored politician persistence to use a schema-aligned upsert path and verified live search remains backed by PostgreSQL.
- Frontend: aligned dashboard search behavior with the API contract so politician and bill search query the backend automatically.
- Mobile: aligned mobile API/mocks with backend field names and search endpoints.
- Data ingestion: added `make ingest-dry-run` to validate sample CSV/JSON files before writing to PostgreSQL.
- Testing: ran frontend/mobile type checks, sample dry-run validation, and backend test/build checks.
- Security: completed Week 1 threat-model review in `docs/THREAT_MODEL.md`.
- Deployment: Week 1 baseline is ready to tag after the current working tree is committed.

## Week 2: API And Ingestion

### Day 8 - Complete
- Backend: profile endpoint now includes office history, election history, citations, trust summary, bills supported/opposed/sponsored, content, and timeline items.
- Frontend: dashboard profile header, overview, and bills tabs prefer live profile API data with sample fallback.
- Mobile: Expo app loads politician profile API data after selecting a politician.
- Data ingestion: politician imports are ready to feed office/election profile joins as those records are normalized.
- Testing: backend tests and frontend/mobile type checks cover the new profile contract.
- Security: UUID handling remains explicit; profile requests reject invalid IDs.
- Deployment: profile endpoint is part of local API smoke checks.

### Day 9 - Complete
- Backend: voting record endpoints now return joined bill/politician display fields and enforce bounded limits.
- Frontend: dashboard voting table uses API-backed joined voting records when available.
- Mobile: voting record screen uses API-backed votes when available.
- Data ingestion: vote records are linked by politician and bill IDs for profile/bill lookups.
- Testing: backend unit/integration checks cover the voting flow.
- Security: vote endpoints clamp `limit` to 1-250 and reject invalid UUIDs.
- Deployment: health-check script now verifies profile and vote endpoint routing.

### Day 10 - Complete
- Backend: bill search now includes sponsor-name matching, and bill detail returns sponsors, cosponsors, actions, citations, and joined votes.
- Frontend: dashboard bill detail pages fetch the aggregate endpoint and show sponsors, cosponsors, actions, citations, and vote groups.
- Mobile: bill search remains API-backed, bill detail fetches API detail/votes, and politician vote/bill cards remain clickable.
- Data ingestion: added `bill_sponsors` schema so future Congress/Open States normalization can persist sponsors and cosponsors.
- Testing: backend bill validation tests and full Gradle checks pass.
- Security: bill search and vote/detail limits remain clamped to bounded ranges.
- Deployment: API docs updated for the bill detail aggregate.

### Day 11 - Complete
- Backend: added public statement search and politician-specific statement endpoints.
- Frontend: dashboard Statements tab uses live public statements first, with sample fallback.
- Mobile: issue/statement screen can display live public statements above stance fallback data.
- Data ingestion: statement records support quotes, venues, source citations, confidence, and type classification.
- Testing: statement type validation test added.
- Security: statement DTO flags suspicious prompt-injection-like scraped text for review.
- Deployment: statement search index added through Liquibase.

### Day 12 - Complete
- Backend: added claims endpoints at `GET /claims` and `GET /politicians/{politicianId}/claims`.
- Frontend: controversies tab reads live claims and displays trust labels, citation counts, fact checks, and review warnings.
- Mobile: profile overview shows claim warnings for allegations, unresolved claims, and non-publishable claim records.
- Data ingestion: claims remain separated by `claim_type`, including allegations and unresolved claims.
- Testing: claim resource validation tests added.
- Security: claim DTOs mark records non-publishable until they have a category and at least one citation.
- Deployment: ready for staging review once seed claim data is loaded.

### Day 13 - Complete
- Backend: added source/citation endpoints at `GET /citations`, `GET /citations/{citationType}/{targetId}`, and `GET /sources`.
- Frontend: citations tab uses live profile citations before falling back to samples.
- Mobile: citations screen uses live profile source citations.
- Data ingestion: existing official/media ingestion writes or emits source citation material for normalization.
- Testing: citation/source validation tests added.
- Security: citation/source DTOs expose source manipulation warnings such as unknown quality or non-HTTPS URLs.
- Deployment: staging seed data can now include source registry and citation rows.

### Day 14 - Complete
- Backend: added `GET /politicians/{politicianId}/timeline`, which aggregates votes, sponsored bills, bill actions, public statements, claims, content/media records, offices, and elections into one chronological profile feed.
- Frontend: dashboard timeline tab now uses the live aggregate endpoint first and falls back to sample timeline records only when the local database has no events.
- Mobile: Expo timeline tab now uses the same live aggregate endpoint and shows event totals, publishable counts, and review-required counts.
- Data ingestion: timeline is computed from normalized PostgreSQL rows rather than a separate duplicate timeline table; official/media imports should keep writing bills, votes, statements, claims, citations, content items, offices, and elections.
- Testing: timeline endpoint is included in backend resource coverage, with sorting and publishability behavior handled in the aggregate service.
- Security: timeline events include publishability and review warnings so unresolved claims, weak citations, and suspicious scraped text are not blended into verified facts.
- Deployment: Week 2 demo can show profile, votes, bills, citations, claims, statements, and timeline using live PostgreSQL data with sample fallback for missing categories.

## Week 3: Accountability Data Graph

Week 3 turns the app from "screens with endpoints" into an accountability engine. The main job is to connect politicians, offices, bills, votes, statements, media records, claims, citations, and timeline events into one source-backed profile.

### Day 15 - Complete
- Backend: added identity resolution service and `GET /identity/politicians/resolve` for matching politicians by exact external identifiers or scored name/state/party/office hints.
- Frontend: identity resolution now returns confidence, reasons, known external identifiers, known social accounts, and `needsReview` for internal/debug use before public display.
- Mobile: no public UI change; mobile benefits from cleaner profile/search data after ingestion links records correctly.
- Data ingestion: expanded `external_identifiers` and added `social_accounts` so Congress.gov, Open States, FEC, Google Civic, media, YouTube, X, Bluesky, RSS, and future sources can link records to the correct politician.
- Testing: added identity endpoint validation and repository coverage for external identifiers plus social accounts.
- Security: weak name-only matches are flagged for review and should not become publishable evidence automatically.
- Deployment: identity matching can now be smoke-tested against seeded politicians such as Congress.gov BioGuide-linked records.

### Day 16 - Complete
- Backend: hardened timeline aggregation with category filtering, dedupe, source/review warnings, and full-timeline stats.
- Frontend: dashboard timeline now uses live aggregate events and category filter chips for votes, bills, statements, claims, media/social content, offices, and elections as data becomes available.
- Mobile: Expo timeline tab now has the same live aggregate behavior and category filters.
- Data ingestion: added X and Bluesky social-post discovery connectors to the existing media ingestion path; posts remain raw citation candidates until normalized.
- Testing: added connector tests for X and Bluesky and kept timeline/API tests passing.
- Security: timeline items preserve `publishable` and warning fields; social posts are not treated as verified facts without account linkage, source citation, and review.
- Deployment: local demo can search live politicians/bills and show timeline fallback today; richer live timelines appear as votes, statements, social posts, and media are normalized into PostgreSQL.

### Day 17 - Complete
- Backend: media connectors for GDELT, Guardian, RSS, YouTube, Bluesky, X, and allowlisted public HTML article events create raw citation candidates and searchable content records.
- Frontend: articles/media records are visible through profile content, timeline, and grouped search results with source/review context.
- Mobile: Expo profile/timeline screens can show the same media records as timeline/content data grows.
- Data ingestion: media records are parsed as citation candidates without converting headlines into facts.
- Testing: connector tests cover article, RSS, YouTube video, social, and safe scraper events.
- Security: prompt-injection-like text is flagged and unreviewed media does not automatically become a claim.
- Deployment: media import can be run in bounded batches with `.env` query limits.

### Internal Review Foundation - Complete
- Backend: added protected `POST /classification/civic` as optional internal review infrastructure, not a public voter-facing judgment endpoint.
- Data model: added `civic_classifications` to persist internal labels, explanations, review status, reviewer metadata, warnings, and model version if the team needs them later.
- Policy: documented the neutrality boundary in `docs/CLASSIFICATION_REVIEW_MODEL.md`.
- Security: harmful-risk, social-media, prompt-injection-like, weak-citation, and unresolved-conduct signals are review-gated.
- Product rule: public screens show evidence, citations, warnings, and source context; voters decide what the evidence means.

### Day 18 - Complete
- Backend: added `GET /search?query=...` for grouped Postgres-backed search across politicians, bills, votes, statements, claims with evidence, media, and citations.
- Frontend: the main search page can display grouped live results while preserving the simple politician-first flow.
- Mobile: existing politician/bill search remains ready to consume the grouped endpoint next.
- Data ingestion: added trigram search-support indexes for fast local search while Elasticsearch/OpenSearch reindexing matures.
- Testing: covered by API/resource and repository tests during Week 3 verification.
- Security: unresolved/no-citation claims are filtered or warning-gated so search does not turn internal drafts into public conclusions.
- Deployment: search troubleshooting is documented in README/API ingestion docs.

### Day 19 - Complete
- Backend: added protected `GET /review/politicians/{id}/completeness` with category counts for biography, offices, elections, votes, sponsored bills, statements, claims, media, citations, and recent activity.
- Frontend: completeness stays internal/admin so voter screens do not imply that missing data means a clean record.
- Mobile: no public completeness score; voter UI stays simple.
- Data ingestion: completeness output identifies source gaps by category.
- Testing: scoring is deterministic and can be smoke-tested against seeded politicians.
- Security: missing records are described as missing records, never as "no issues" or "no controversies."
- Deployment: admins can generate one-profile demo readiness reports locally.

### Day 20 - Complete
- Backend: trust scoring remains evidence-quality based; review metadata can be persisted on claims, statements, and content without public value judgments.
- Frontend: claims/statements/citations display confidence, citation count, source quality, and neutral warnings.
- Mobile: profile/timeline responses include confidence and review metadata for compact voter display.
- Data ingestion: imported records can be marked system-generated and reviewed later through the protected workflow.
- Testing: existing trust-score tests protect source-quality, citation-count, and recency behavior.
- Security: manual trust/review changes should continue to produce audit events as admin workflows expand.
- Deployment: trust scoring and review behavior are documented for staging.

### Day 21 - Complete
- Backend: added protected `GET /review/queue` plus review workflow fields for claims, public statements, and content items.
- Frontend: internal review can use the protected queue; public voter tabs avoid classification labels and show source/review context.
- Mobile: no admin controls.
- Data ingestion: unreviewed media and claims stay internal or clearly warning-gated.
- Testing: publishability logic is represented through review warnings for missing citations, weak source quality, unresolved allegations, and suspicious text.
- Security: `review` and `classification` endpoints are admin-token protected.
- Deployment: Week 3 accountability data demo now has search, timeline, source context, review queue, and completeness checks.

## Week 4: Voter Experience, Safety, And Launch Readiness

Week 4 makes the app understandable and trustworthy for real voters. The public experience should answer: Who is this politician? What have they voted for or against? What bills did they sponsor? What have they said? What credible records support this?

### Day 22
- Backend: harden authorization, CORS, production config, and admin-only routes.
- Frontend: remove internal Security tab from public mode and keep it available only for internal operators.
- Mobile: confirm no internal/admin views are exposed.
- Data ingestion: isolate production import credentials by provider and source.
- Testing: security config tests and admin-route authorization tests.
- Security: dependency scan, container scan, and secrets review.
- Deployment: staging hardening checklist.

### Day 23
- Backend: add abuse-prevention request telemetry and rate limits for public search endpoints.
- Frontend: graceful 429, empty, and degraded-data states.
- Mobile: offline/error states for search, profile, bills, and citations.
- Data ingestion: enforce per-source request limits and backoff.
- Testing: rate-limit and degraded API tests.
- Security: suspicious scraping/search pattern detection.
- Deployment: metrics dashboard for API/search/import health.

### Day 24
- Backend: add privacy classification and public-record safety checks.
- Frontend: hide or flag sensitive/private data before display.
- Mobile: hide or flag sensitive/private data before display.
- Data ingestion: reject private contact info, accidental personal data, or irrelevant private records unless explicitly allowed and lawful.
- Testing: privacy validator tests.
- Security: privacy review workflow and public-record-only policy.
- Deployment: privacy policy draft and source-use policy draft.

### Day 25
- Backend: finalize saved politicians strategy: local-first for MVP, backend sync only if accounts are enabled.
- Frontend: saved politicians view shows latest activity and data gaps.
- Mobile: saved politicians remains fast and local-first.
- Data ingestion: no new source scope; focus on reliable seed dataset.
- Testing: saved-state and profile refresh tests.
- Security: account data access review if sync is enabled.
- Deployment: mobile backend config freeze.

### Day 26
- Backend: OpenAPI documentation and contract tests for public endpoints.
- Frontend: API error telemetry hooks and final public copy review.
- Mobile: API error telemetry hooks and final public copy review.
- Data ingestion: importer operator guide for official, civic, and media sources.
- Testing: contract tests for search, profile, bills, claims, citations, and timeline.
- Security: review API docs for exposed internals.
- Deployment: publish internal API docs.

### Day 27
- Backend: performance tune search, profile aggregation, bill detail, timeline, and citation queries.
- Frontend: loading states, empty states, and accessibility pass.
- Mobile: loading states, empty states, device QA, and accessibility pass.
- Data ingestion: final seed import for target state/federal officials.
- Testing: load tests and end-to-end smoke tests.
- Security: verify audit coverage for imports, source changes, and trust changes.
- Deployment: staging performance run.

### Day 28
- Backend: production deployment rehearsal and rollback plan.
- Frontend: production web build and staging acceptance pass.
- Mobile: Expo preview builds for internal testing.
- Data ingestion: rehearsal import and reindex.
- Testing: smoke tests on preview builds.
- Security: app store privacy review and incident response checklist.
- Deployment: create release checklist.

### Day 29
- Backend: fix release-blocking bugs and freeze public API surface.
- Frontend: fix release-blocking bugs and freeze public UI.
- Mobile: app store metadata, screenshots, privacy labels, and build submission prep.
- Data ingestion: freeze MVP dataset and document known data gaps.
- Testing: full regression suite.
- Security: final dependency scan and final secrets review.
- Deployment: tag release candidate.

### Day 30
- Backend: launch monitoring for API errors, import failures, search failures, and slow profile responses.
- Frontend: monitor web behavior and search/profile drop-offs.
- Mobile: monitor app review feedback, Expo build health, and crash reports.
- Data ingestion: monitor import errors and source rate limits.
- Testing: post-launch regression.
- Security: review abuse signals, audit events, and misinformation-risk reports.
- Deployment: post-launch retrospective and next 30-day scope.

## Definition Of Done

- Web dashboard navigates across all sections.
- Mobile MVP runs locally in Expo and has store-ready config.
- Core API endpoints return JSON DTOs.
- Data ingestion imports validated CSV/JSON files.
- PostgreSQL is source of truth.
- Search indexes can be rebuilt.
- Every public claim has citations, source context, and review/publishability status.
- Admin changes are auditable.
- Production secrets are not committed.
- Staging deploy has repeatable migration and smoke-test steps.
