# Staging Performance Runbook

Day 27 adds basic repeatable performance checks for the MVP read paths. This is not a replacement for full load testing; it is the staging smoke layer that tells us whether search/profile/bill/timeline routes are still healthy.

## Hot Paths

- `GET /search`
- `GET /politicians/search/name`
- `GET /politicians/{id}/profile`
- `GET /politicians/{id}/timeline`
- `GET /bills/search`
- `GET /bills/{id}`
- `GET /claims`
- `GET /citations`

The migration `20260608_0012_performance_indexes.sql` adds composite indexes for votes, bills, bill actions, citations, statements, claims, content, imports, and audit lookups.

## Runbook

1. Deploy or start the API.
2. Apply migrations with `make db-migrate`.
3. Run API smoke checks:

```sh
scripts/api-smoke-test.sh
```

4. Run the lightweight search load smoke:

```sh
REQUESTS=30 QUERY="Trump" scripts/search-load-smoke.sh
```

5. Inspect protected metrics:

```sh
curl -H "X-Admin-Token: $ADMIN_API_TOKEN" "$API_BASE/review/metrics"
```

## Pass Criteria

- Public smoke endpoints return `2xx`.
- Protected review metrics return `401` without a token and `200` with a token.
- Search load smoke has no `5xx` responses.
- Occasional `429` responses are acceptable if rate limiting is intentionally enabled and the UI handles them gracefully.
- Profile and timeline responses should return quickly enough for an interactive search flow once local seed data is loaded.

## Follow-Up Metrics To Add

- p50/p95 route latency by endpoint.
- Search result hit rate by source type.
- Import batch duration and failed-row count.
- Timeline event count by category.
- Slow query logging for profile and bill aggregates.
