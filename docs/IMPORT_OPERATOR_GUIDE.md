# Import Operator Guide

This guide is for local/staging operators who load official, civic, media, and social discovery data into PostgreSQL.

## Preflight

1. Confirm `.env` exists and contains local database settings plus provider keys.
2. Start infrastructure with `make dev`.
3. Apply migrations with `make db-migrate`.
4. Validate changelogs with `make db-validate`.
5. Keep `.env` out of Git. Only `.env.example` should be committed.

## Official And Civic Imports

Use official/public-record sources first because they carry the strongest evidence value:

```sh
make ingest-congress-members
make ingest-congress-bills
make ingest-govinfo-packages
make ingest-state-civic
make ingest-federal-executives
```

Expected outcome:

- Politicians are upserted into `politicians`.
- External identifiers link records across sources.
- Bills, votes, bill actions, offices, elections, citations, and content records fill profile/timeline aggregates as they normalize.
- Import batches and row results make failures inspectable.

## Local File Imports

Use templates under `data/templates/` for controlled CSV/JSON imports:

```sh
make ingest-dry-run
make ingest-local
```

Dry runs should be clean before writing data. Failed rows should be fixed in the source file rather than patched manually in the database.

## Media And Social Discovery

Media/social jobs gather citation candidates. They do not automatically turn headlines, posts, or article text into verified facts.

```sh
make ingest-media
```

Discovery sources include GDELT, Guardian, RSS, YouTube, Bluesky, X, and allowlisted HTML pages. Treat these records as evidence candidates until they are linked to politicians, bills, statements, citations, and review metadata.

## After Each Import

Run:

```sh
make health-check
scripts/api-smoke-test.sh
```

Then check:

- Search returns expected politicians and bills.
- `GET /politicians/{id}/profile` has offices, votes, bills, citations, or clear data gaps.
- `GET /politicians/{id}/timeline` shows normalized activity.
- Protected import/review/audit endpoints still require `X-Admin-Token`.

## Safety Rules

- Prefer official sources for facts, votes, bill text, and office history.
- Keep media/social records as citation candidates until reviewed.
- Do not import private contact details, sensitive identifiers, or non-public personal data.
- Do not present missing data as a clean record.
- Keep every operator write traceable through import rows and audit events.
