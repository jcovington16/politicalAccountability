# API Metrics And Abuse Prevention

The MVP uses lightweight in-memory API telemetry and request limits. This is enough for local/staging demos; production should eventually move these counters to gateway logs, Prometheus, OpenTelemetry, or the hosting provider's edge metrics.

## Rate-Limited Routes

The API rate-limits public search-style endpoints:

- `GET /search`
- `GET /politicians/search/...`
- `GET /bills/search`
- `GET /citations`

Configure with:

```sh
RATE_LIMIT_ENABLED=true
PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE=60
```

When a client exceeds the limit, the API returns:

```text
HTTP 429
Retry-After: 60
```

The web and mobile apps show a friendly "try again in a minute" message.

## Protected Metrics

Use:

```sh
GET /review/metrics
X-Admin-Token: <ADMIN_API_TOKEN>
```

The response includes process start time, route count buckets, HTTP status count buckets, and rate-limited request totals.

## Suspicious Patterns

Review these patterns before expanding public launch:

- repeated searches from one IP
- very high query volume with no profile opens
- bursts of names across many jurisdictions
- repeated `429` responses
- ingestion provider `429` or `5xx` retries
- unknown-source media records that later become claims

## Ingestion Backoff

Provider connectors retry transient `429` and `5xx` responses with bounded backoff.

```sh
INGEST_HTTP_MAX_ATTEMPTS=3
INGEST_HTTP_BACKOFF_MS=1000
```

Keep provider-specific limits conservative. Open States is especially strict in the current setup, so avoid wide loops until caching and queueing are stronger.
