# Day 1 Foundation

Day 1 locks in the product rules, schema direction, ingestion contract, and local operating path for the state and federal MVP.

## Product Rule

Public Record is neutral by design:

```text
We do not score politicians by ideology.
We score information by evidence quality.
```

The app should help voters decide for themselves by showing public actions, votes, legislation, statements, citations, public articles, positive accomplishments, controversies, unresolved claims, and election context. The platform should present the same structure for every state and federal politician regardless of party, office, branch, ideology, or popularity.

## MVP Scope

Day 1 scope is state and federal level public officials across the three branches of government:

- Executive: governors, lieutenant governors, attorneys general, secretaries of state, federal executive officials where public records are available.
- Legislative: state legislators, U.S. representatives, and U.S. senators.
- Judicial: elected or appointed judges where public record data is legally available and can be cited.

Local officials remain an intended future expansion. The schema should not block local offices, but ingestion and UI work should prioritize state and federal data first.

## Public vs Internal Views

The public voter app should not expose builder/security controls. Voter-facing navigation should focus on:

```text
Overview
Votes
Bills
Statements
News
Controversies
Accomplishments
Citations
Timeline
Compare
```

The current dashboard Security tab is intentionally internal. It is a builder/admin checklist for data integrity, misinformation risk, source manipulation, prompt injection, scraping risks, privacy, authorization, audit logs, and abuse prevention.

## Schema Gap Map

Existing schema coverage:

- `politicians`
- `bills`
- `votes`
- `media_files`
- `content_items`
- `provenance`
- `news_articles`

Required MVP schema gaps:

| Area | Tables Needed | Why It Matters |
| --- | --- | --- |
| Offices | `offices`, `politician_offices` | Tracks branch, jurisdiction, office level, district, term dates, and current representation. |
| Elections | `elections`, `election_candidates` | Enables compare-by-seat and candidate context for upcoming races. |
| Public statements | `public_statements` | Separates direct quotes, speeches, interviews, press releases, and social posts from generic content. |
| Source citations | `source_registry`, `source_citations` | Reuses citations across votes, bills, articles, statements, controversies, and fact checks. |
| Fact checks | `claims`, `fact_checks` | Separates verified facts, unresolved claims, allegations, and corrections. |
| Tags | `tags`, `taggings` | Supports issues, topics, policy areas, and searchable profile grouping. |
| Audit history | `audit_log` | Records who changed claims, citations, trust labels, and source quality decisions. |
| Imports | `import_batches`, `import_row_results` | Tracks file hashes, skipped rows, validation errors, and import lineage. |

Day 2 should start with offices and elections because they unlock state/federal scope and compare-by-seat.

## API Contract Needs

The web dashboard and mobile MVP need these contracts next:

- Politician search: query by name, office, state, branch, party, district, and election seat.
- Politician profile: biography, offices held, current office, election history, trust summary, citations.
- Voting record: vote, bill, date, chamber, session, citation, and issue tags.
- Bills: sponsor/cosponsor/support/opposition relationship, status, dates, summaries, citations.
- Statements: statement type, direct quote flag, venue, date, source citation, confidence.
- Timeline: normalized public activity across votes, bills, statements, articles, fact checks, and controversies.
- Compare: politicians running for the same seat or holding comparable offices.
- Citations: source quality, URL, publisher, retrieved date, archive URL, citation count, and confidence.

## Roles

| Role | Purpose | Access |
| --- | --- | --- |
| Public reader | Voter using web or mobile app. | Read published records only. No writes. |
| Analyst | Trusted reviewer who can classify records and suggest corrections. | Read drafts, propose edits, classify claims, attach citations. |
| Data import operator | Runs CSV/JSON imports and resolves row-level validation errors. | Create import batches, upload source files, retry failed rows. |
| Admin | Manages publishing, trust overrides, source quality overrides, and user access. | Full moderation, publishing, role assignment, and audit access. |

Security rule: every write by analyst, data-import operator, or admin must eventually produce an audit event.

## Canonical Ingestion Templates

Initial templates live in `data/templates/`:

- `politicians.csv`
- `bills.csv`
- `votes.csv`
- `news_articles.json`

Rules:

- Required identifiers must be stable and source-specific where possible.
- Every imported row needs source provenance.
- Invalid rows should be skipped with a row-level error, not silently coerced.
- PostgreSQL remains the source of truth; search indexes are rebuildable projections.

## Local Development Boot Sequence

1. Start infrastructure:

```sh
make dev
```

2. Apply migrations:

```sh
make db-validate
make db-migrate
```

3. Run backend tests:

```sh
make test
```

4. Start API:

```sh
make run
```

5. Start dashboard:

```sh
make dashboard-dev
```

6. Start mobile app:

```sh
make mobile-start
```

7. Import local files when ready:

```sh
make ingest-local dir=data/templates
```

## Day 1 Completion Criteria

- Schema gaps are identified and ordered.
- Dashboard navigation has been verified.
- Mobile MVP screen list and app-store bundle identifiers are documented.
- Canonical CSV/JSON templates exist.
- CI validates Liquibase changelogs.
- Roles are defined.
- Local boot sequence is documented.
