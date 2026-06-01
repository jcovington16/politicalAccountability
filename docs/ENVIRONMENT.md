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
| `LIQUIBASE_IMAGE` | Liquibase scripts | Optional | `liquibase:4.33` | Docker fallback image. |
| `LIQUIBASE_DOCKER_NETWORK` | Liquibase scripts | CI | unset | Use `host` in GitHub Actions service-container validation. |
| `NVD_API_KEY` | CI security scan | Recommended | unset | Optional GitHub secret for OWASP Dependency-Check. Reduces NVD throttling and 403 failures. |

Production should split migration credentials from runtime credentials. Liquibase should use a migration account; API and ingestion services should use least-privilege runtime accounts.
