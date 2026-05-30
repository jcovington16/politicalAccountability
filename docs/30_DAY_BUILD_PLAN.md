# 30-Day Build Plan

Goal: ship a credible web and mobile MVP for political accountability with reliable data ingestion, transparent citations, trust scoring, and a secure operational foundation.

## Week 1: Foundation And Data Model

### Day 1
- Backend: finalize schema gaps for offices, elections, public statements, source citations, fact checks, tags, and audit logs.
- Frontend: confirm dashboard navigation and data contract needs.
- Mobile: confirm MVP screen list and app-store bundle identifiers.
- Data ingestion: define canonical CSV/JSON templates.
- Testing: add schema migration validation to CI.
- Security: define roles for public reader, analyst, admin, and data-import operator.
- Deployment: document local dev boot sequence.

### Day 2
- Backend: add Liquibase migrations for `offices`, `elections`, and `politician_offices`.
- Frontend: add API DTO expectations for office and election history.
- Mobile: add API client shell.
- Data ingestion: add validation rules for politician identity matching.
- Testing: add repository integration tests for office/election tables.
- Security: require source provenance for all ingested records.
- Deployment: add environment variable inventory.

### Day 3
- Backend: add `source_registry` and `source_citations` tables.
- Frontend: make citations reusable across profile, votes, bills, statements, and controversies.
- Mobile: wire source citations screen to future endpoint shape.
- Data ingestion: normalize source names and URLs.
- Testing: add citation validation tests.
- Security: add source quality enum and admin override audit requirement.
- Deployment: add staging database migration checklist.

### Day 4
- Backend: add `public_statements`, `claims`, and `fact_checks` tables.
- Frontend: split public statements from generic content items.
- Mobile: add issue stance API model.
- Data ingestion: define statement import template.
- Testing: add parsing tests for quotes, claims, and statement dates.
- Security: define prompt-injection sanitation requirements for scraped text.
- Deployment: add backup/restore notes for Postgres.

### Day 5
- Backend: add `audit_log` append-only table and repository.
- Frontend: add admin-facing audit event type definitions.
- Mobile: keep audit internal; no voter-facing exposure yet.
- Data ingestion: log import batch metadata.
- Testing: add audit write tests.
- Security: define audit retention and tamper-resistance rules.
- Deployment: add log retention defaults.

### Day 6
- Backend: add `import_batches` and `import_row_results`.
- Frontend: add import status wireframe for later admin UI.
- Mobile: no mobile work unless week slips.
- Data ingestion: report skipped rows with reasons.
- Testing: add integration test for one full import batch.
- Security: hash source files and store checksum.
- Deployment: document import rollback procedure.

### Day 7
- Backend: refactor repositories around the finalized schema.
- Frontend: align dashboard mocks with final API contracts.
- Mobile: align mobile mocks with final API contracts.
- Data ingestion: dry-run sample data through validators.
- Testing: run full unit and integration suite.
- Security: threat-model review.
- Deployment: tag Week 1 baseline.

## Week 2: API And Ingestion

### Day 8
- Backend: add politician profile endpoint with offices, elections, citations, and trust summary.
- Frontend: replace sample profile data with API data.
- Mobile: implement profile API fetch.
- Data ingestion: import politicians with office history.
- Testing: API resource tests for profile endpoint.
- Security: validate UUID/query params consistently.
- Deployment: deploy API to staging.

### Day 9
- Backend: add voting record endpoint with bill joins and citations.
- Frontend: wire voting table to API.
- Mobile: wire voting record screen.
- Data ingestion: import votes with bill references.
- Testing: integration tests for vote lookups.
- Security: protect against large unbounded queries.
- Deployment: add API health checks for vote endpoint.

### Day 10
- Backend: add bills search/detail endpoints with sponsor/cosponsor support.
- Frontend: wire bills supported/opposed views.
- Mobile: wire compare and bills-related stances.
- Data ingestion: import bills and sponsors.
- Testing: bill search tests.
- Security: rate-limit search endpoint design.
- Deployment: update API docs.

### Day 11
- Backend: add public statements endpoint.
- Frontend: wire statements tab.
- Mobile: wire issue stance screen from statements and votes.
- Data ingestion: import statements and quotes.
- Testing: statement validation tests.
- Security: strip or quarantine suspicious scraped instructions.
- Deployment: add search index mapping for statements.

### Day 12
- Backend: add controversies/claims endpoint.
- Frontend: wire controversies tab with trust labels.
- Mobile: add claim warnings to profile.
- Data ingestion: import allegations and unresolved claims separately.
- Testing: trust classification tests.
- Security: require claim category and citation before publishing.
- Deployment: staging review.

### Day 13
- Backend: add source citation endpoint and source registry endpoint.
- Frontend: wire citations tab.
- Mobile: wire source citations screen.
- Data ingestion: normalize citations.
- Testing: citation count and recency tests.
- Security: source manipulation anomaly checks.
- Deployment: update staging seed data.

### Day 14
- Backend: add timeline aggregation endpoint.
- Frontend: wire timeline.
- Mobile: wire timeline.
- Data ingestion: produce timeline events after imports.
- Testing: timeline sorting tests.
- Security: ensure timeline only uses publishable records.
- Deployment: Week 2 demo.

## Week 3: Trust, Security, And Product Polish

### Day 15
- Backend: persist trust scores for claims/statements/content.
- Frontend: show confidence and explanation everywhere public.
- Mobile: show confidence labels in voter-friendly language.
- Data ingestion: assign initial trust scores during import.
- Testing: score regression tests.
- Security: manual override requires audit event.
- Deployment: trust scoring config in staging.

### Day 16
- Backend: add admin-only trust override endpoint.
- Frontend: create internal moderation sketch or hidden route.
- Mobile: no admin controls.
- Data ingestion: mark importer-generated scores as system decisions.
- Testing: authorization tests.
- Security: RBAC middleware.
- Deployment: secrets and auth config in staging.

### Day 17
- Backend: add audit event endpoint for admins.
- Frontend: audit log viewer.
- Mobile: no voter-facing audit log.
- Data ingestion: import actions write audit events.
- Testing: audit immutability tests.
- Security: append-only audit checks.
- Deployment: audit log retention config.

### Day 18
- Backend: add abuse-prevention request telemetry.
- Frontend: handle 429 and API error states gracefully.
- Mobile: handle offline/error states.
- Data ingestion: enforce per-source rate limits.
- Testing: rate-limit tests.
- Security: suspicious search pattern detection.
- Deployment: dashboard metrics.

### Day 19
- Backend: add privacy classification fields.
- Frontend: hide or flag sensitive data.
- Mobile: hide or flag sensitive data.
- Data ingestion: reject private contact info unless explicitly allowed.
- Testing: privacy validator tests.
- Security: privacy review workflow.
- Deployment: privacy policy draft.

### Day 20
- Backend: harden CORS/auth/config defaults.
- Frontend: production API config.
- Mobile: production API config.
- Data ingestion: production import credentials isolated.
- Testing: security config tests.
- Security: dependency scan and container scan.
- Deployment: staging hardening checklist.

### Day 21
- Backend: fix bugs from Week 3 testing.
- Frontend: responsive QA.
- Mobile: device QA pass.
- Data ingestion: sample import replay.
- Testing: full regression.
- Security: tabletop review of misinformation abuse cases.
- Deployment: release candidate branch.

## Week 4: Launch Readiness

### Day 22
- Backend: performance tune core queries.
- Frontend: add loading and empty states.
- Mobile: add loading and empty states.
- Data ingestion: add reindex command.
- Testing: load test search/profile endpoints.
- Security: verify audit coverage.
- Deployment: staging performance run.

### Day 23
- Backend: OpenAPI documentation.
- Frontend: API error telemetry hooks.
- Mobile: API error telemetry hooks.
- Data ingestion: importer operator guide.
- Testing: contract tests.
- Security: review API docs for exposed internals.
- Deployment: API docs published internally.

### Day 24
- Backend: finalize saved politicians sync endpoint if accounts are enabled.
- Frontend: saved politician view backed by API or local storage.
- Mobile: saved politicians local-first.
- Data ingestion: no new scope.
- Testing: saved-state tests.
- Security: account data access review.
- Deployment: mobile backend config freeze.

### Day 25
- Backend: freeze schema for MVP.
- Frontend: final visual polish.
- Mobile: final visual polish.
- Data ingestion: final seed dataset import.
- Testing: end-to-end smoke tests.
- Security: final secrets review.
- Deployment: staging release candidate.

### Day 26
- Backend: production deployment rehearsal.
- Frontend: production web build.
- Mobile: Expo preview builds.
- Data ingestion: rehearsal import.
- Testing: smoke tests on preview builds.
- Security: app store privacy review.
- Deployment: create release checklist.

### Day 27
- Backend: fix release-blocking bugs.
- Frontend: fix release-blocking bugs.
- Mobile: fix release-blocking bugs.
- Data ingestion: verify import reproducibility.
- Testing: regression suite.
- Security: dependency scan.
- Deployment: tag release candidate.

### Day 28
- Backend: production readiness review.
- Frontend: final accessibility pass.
- Mobile: app store metadata, screenshots, privacy labels.
- Data ingestion: import monitoring plan.
- Testing: manual acceptance testing.
- Security: abuse prevention review.
- Deployment: submit internal mobile builds.

### Day 29
- Backend: launch freeze.
- Frontend: launch freeze.
- Mobile: submit to Google Play and Apple App Store if accounts are ready.
- Data ingestion: freeze MVP dataset.
- Testing: final smoke test.
- Security: incident response checklist.
- Deployment: production deploy window.

### Day 30
- Backend: monitor launch metrics and errors.
- Frontend: monitor web behavior.
- Mobile: monitor app review feedback and crash reports.
- Data ingestion: monitor import errors.
- Testing: post-launch regression.
- Security: review abuse signals and audit events.
- Deployment: post-launch retrospective and next 30-day scope.

## Definition Of Done

- Web dashboard navigates across all sections.
- Mobile MVP runs locally in Expo and has store-ready config.
- Core API endpoints return JSON DTOs.
- Data ingestion imports validated CSV/JSON files.
- PostgreSQL is source of truth.
- Search indexes can be rebuilt.
- Every public claim has classification and citations.
- Admin changes are auditable.
- Production secrets are not committed.
- Staging deploy has repeatable migration and smoke-test steps.
