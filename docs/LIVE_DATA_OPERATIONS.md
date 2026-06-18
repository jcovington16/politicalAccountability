# Live Data Operations

The public web and mobile clients display only records returned by the API. An empty database, an empty search result, and an unavailable API are shown honestly; none of those states substitutes demo politicians or fabricated profile details.

## Kafka Health

Kafka and ZooKeeper use named volumes, restart automatically after ordinary failures, and expose Compose health checks. The API gateway starts only after Kafka reports healthy.

Verify the complete message path:

```sh
make kafka-health
```

This creates a temporary topic, produces a unique message, consumes the same message, and deletes the topic. If local broker metadata becomes inconsistent, recover only the event-streaming state:

```sh
make kafka-recover
make kafka-health
```

`kafka-recover` preserves PostgreSQL, Elasticsearch, MinIO, and their application data. It deletes local Kafka/ZooKeeper data, so do not use it against a shared or production cluster.

## Populate Live Profiles

Run the primary-source refresh in identity-first order:

```sh
make refresh-live-data
```

The sequence applies migrations, imports federal executives and Congress members, imports state/civic records, then fetches and normalizes Congress bills and GovInfo records into PostgreSQL. Each normalized source is fetched once per run to preserve provider quota. Media is intentionally optional because it is broader, less authoritative, and can consume provider quota:

```sh
INCLUDE_MEDIA=true make refresh-live-data
```

The application searches PostgreSQL first. Connector results must be normalized and associated with stable politician IDs before they become complete profile records. Kafka transports ingestion events; PostgreSQL remains the searchable system of record.

Host-side database tools use `localhost:5434` by default to avoid colliding with a native PostgreSQL installation. Compose services continue to use `postgres:5432`. Override `POSTGRES_HOST_PORT` and `DB_PORT` together if port 5434 is unavailable.

The current refresh broadens searchable identities and legislation, but it does not yet make every profile complete. Office terms, elections, roll-call votes, sponsor/cosponsor relationships, public statements, and reviewed media each require their own normalized importer and stable identity links. The completeness report is the release gate for those dimensions.

## Measure Completeness

After a refresh, inspect named profiles:

```sh
make profile-completeness names="Marco Rubio,Donald Trump"
```

The report searches stored live profiles and calls the protected completeness endpoint for every match. It reports coverage and the next ingestion action for biography, offices, elections, votes, sponsored bills, statements, claims, media, citations, and recent activity.

Recommended operating loop:

1. Refresh official identity and office data.
2. Normalize bills, votes, sponsorships, and citations.
3. Run the completeness report for high-priority races and officials.
4. Ingest only the missing dimensions from authoritative sources.
5. Resolve weak identity matches in the review queue.
6. Refresh media after official identities are stable.
7. Re-run completeness and publish only reviewed records.

Completeness is coverage, not a judgment about a politician. Missing records must remain visibly missing rather than being interpreted as a clean history.
