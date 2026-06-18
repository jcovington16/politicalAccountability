#!/usr/bin/env bash
set -euo pipefail

RUN_LIVE_CHECKS="${RUN_LIVE_CHECKS:-false}"

echo "Running release-candidate checks..."

./gradlew :api-gateway:test :common:test :ingestion-service:test

DATABASE_URL="${DATABASE_URL:-offline:postgresql}" ./scripts/db/liquibase.sh validate
node scripts/validate-sample-data.mjs data/templates

(
  cd dashboard
  npm run build
)

(
  cd mobile
  npm run typecheck
)

if [ "${RUN_LIVE_CHECKS}" = "true" ]; then
  scripts/api-smoke-test.sh
  REQUESTS="${REQUESTS:-30}" scripts/search-load-smoke.sh
else
  echo "Skipping live API smoke checks. Set RUN_LIVE_CHECKS=true when the API is running."
fi

echo "Release-candidate checks completed."
