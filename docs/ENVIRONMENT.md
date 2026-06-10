# Environment Inventory

Local development uses `.env` for scripts and service configuration. Do not commit real secrets.

| Variable | Used By | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `DB_HOST` | Liquibase scripts | Local | `localhost` | Host for PostgreSQL. |
| `DB_PORT` | Liquibase scripts | Local | `5432` | Port for PostgreSQL. |
| `DB_NAME` | Liquibase scripts | Local | `political_data` | Database name. |
| `DB_USERNAME` | Liquibase scripts | Local | `postgres` | Migration database user. |
| `DB_PASSWORD` | Liquibase scripts | Local | `postgres` | Use secrets manager outside local dev. |
| `DATABASE_URL` | API, ingestion, Liquibase | Yes | `jdbc:postgresql://localhost:5432/political_data` | Full JDBC URL override. |
| `DATABASE_USER` | API, ingestion | Yes | `postgres` | Runtime database user. |
| `DATABASE_PASSWORD` | API, ingestion | Yes | `postgres` | Runtime database password. |
| `ELASTICSEARCH_URL` | Ingestion/search | Local | `http://localhost:9200` | Search index endpoint. |
| `OPENSEARCH_URL` | Ingestion/search | Optional | unset | Fallback if Elasticsearch URL is absent. |
| `KAFKA_BOOTSTRAP_SERVERS` | Storage/streaming | Local | `localhost:9092` | Used by event consumers/producers. |
| `MINIO_ENDPOINT` | Storage/media | Local | `http://localhost:9000` | Local object storage endpoint. |
| `MINIO_ACCESS_KEY` | Storage/media | Local | `minioadmin` | Local only. |
| `MINIO_SECRET_KEY` | Storage/media | Local | `minioadmin` | Local only. |
| `INGEST_INPUT_DIR` | Local ingestion | Optional | `data/ingestion` | Directory containing CSV/JSON imports. |
| `ADMIN_API_TOKEN` | API admin endpoints | Yes for shared envs | `local-admin-token` locally | Required in `X-Admin-Token` for `/imports` and `/audit-log`. Use a long random secret outside local dev. |
| `OPENSTATES_JURISDICTION` | Open States ingestion | Optional | `ocd-jurisdiction/country:us/state:co/government` | State jurisdiction to seed. |
| `OPENSTATES_SESSION` | Open States ingestion | Optional | unset | Restrict bill pulls to one legislative session. |
| `OPENSTATES_LIMIT` | Open States ingestion | Optional | `25` | Max people/bills pulled per run. |
| `GOOGLE_CIVIC_ADDRESS` | Google Civic ingestion | Optional | unset | Address used to fetch representative context. |
| `GDELT_QUERIES` | Media ingestion | Optional | unset | Comma-separated public-media search queries. No API key required. |
| `GDELT_MAX_RECORDS` | Media ingestion | Optional | `25` | Max GDELT article records per query. |
| `RSS_FEEDS` | Media ingestion | Optional | unset | Comma-separated feed URLs. Use `url|Source Name` to label a feed. |
| `GUARDIAN_API_KEY` | Media ingestion | Optional | unset | Server-side Guardian Open Platform API key. |
| `GUARDIAN_QUERIES` | Media ingestion | Optional | unset | Comma-separated Guardian article search queries. |
| `GUARDIAN_PAGE_SIZE` | Media ingestion | Optional | `10` | Max Guardian articles per query. |
| `YOUTUBE_API_KEY` | Media ingestion | Optional | unset | Server-side YouTube Data API key. Required for YouTube ingestion only. |
| `YOUTUBE_QUERIES` | Media ingestion | Optional | unset | Comma-separated public YouTube search queries. |
| `YOUTUBE_CHANNEL_IDS` | Media ingestion | Optional | unset | Comma-separated official/public channel IDs to ingest from. |
| `YOUTUBE_MAX_RESULTS` | Media ingestion | Optional | `10` | Max YouTube videos per query/channel per run. |
| `NEWS_SCRAPER_ENABLED` | Media ingestion | Optional | `false` | Enables explicit public article URL scraping. Keep off unless sources are reviewed. |
| `NEWS_SCRAPER_URLS` | Media ingestion | Optional | unset | Comma-separated `url|Source Name` article URLs. No bulk crawling. |
| `NEWS_SCRAPER_MAX_PAGES` | Media ingestion | Optional | `10` | Max explicit article pages per run. |
| `PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE` | API abuse prevention | Optional | `60` | Per-client public search request limit. |
| `RATE_LIMIT_ENABLED` | API abuse prevention | Optional | `true` | Enables in-memory public search rate limits. |
| `INGEST_HTTP_MAX_ATTEMPTS` | Ingestion backoff | Optional | `3` | Max attempts for transient provider `429` and `5xx` responses. |
| `INGEST_HTTP_BACKOFF_MS` | Ingestion backoff | Optional | `1000` | Initial backoff delay for transient provider responses. |
| `LIQUIBASE_IMAGE` | Liquibase scripts | Optional | `liquibase:4.33` | Docker fallback image. |
| `LIQUIBASE_DOCKER_NETWORK` | Liquibase scripts | CI | unset | Use `host` in GitHub Actions service-container validation. |
| `NVD_API_KEY` | CI security scan | Recommended | unset | Optional GitHub secret for OWASP Dependency-Check. Reduces NVD throttling and 403 failures. |

Production should split migration credentials from runtime credentials. Liquibase should use a migration account; API and ingestion services should use least-privilege runtime accounts.
