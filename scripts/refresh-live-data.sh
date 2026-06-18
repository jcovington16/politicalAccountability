#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

echo "Applying database migrations..."
./scripts/db/liquibase.sh update

# Run primary/official identity and legislative sources before media discovery so
# later records can resolve to stable politician IDs instead of name-only matches.
echo "Refreshing federal executive profiles..."
./gradlew :ingestion-service:runFederalExecutiveSeed
echo "Refreshing Congress member profiles..."
./gradlew :ingestion-service:runCongressGovMembers
echo "Refreshing state and civic records..."
./gradlew :ingestion-service:runStateCivicIngestion
# Normalization fetches Congress.gov and GovInfo once and writes their official
# records to PostgreSQL. The separate raw connector targets are diagnostics and
# are intentionally not repeated here, which preserves provider quota.
echo "Fetching and normalizing Congress.gov and GovInfo records into PostgreSQL..."
./gradlew :ingestion-service:runOfficialDataNormalization

if [[ "${INCLUDE_MEDIA:-false}" == "true" ]]; then
  echo "Refreshing media records..."
  ./gradlew :ingestion-service:runMediaIngestion
else
  echo "Media refresh skipped. Run with INCLUDE_MEDIA=true when desired."
fi

echo "Live-data refresh completed. Run make profile-completeness names='Name One,Name Two'."
