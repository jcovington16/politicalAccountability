#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
ADMIN_BASE="${ADMIN_BASE:-http://localhost:8081}"
ADMIN_API_TOKEN="${ADMIN_API_TOKEN:-}"
QUERY="${QUERY:-Trump}"

echo "Launch monitor snapshot"
echo "API_BASE=${API_BASE}"
echo "ADMIN_BASE=${ADMIN_BASE}"

check_status() {
  local name="$1"
  local url="$2"
  local status
  status="$(curl -sS -o /dev/null -w '%{http_code}' "${url}" || true)"
  echo "${name}: HTTP ${status}"
  case "${status}" in
    200|204|400|401|404|429) return 0 ;;
    *) return 1 ;;
  esac
}

check_status "Admin health" "${ADMIN_BASE}/healthcheck"
check_status "Grouped search" "${API_BASE}/search?query=${QUERY}&limit=5"
check_status "Politician search" "${API_BASE}/politicians/search/name?name=${QUERY}"
check_status "Bill search" "${API_BASE}/bills/search?query=health&limit=5"
check_status "Protected review metrics without token" "${API_BASE}/review/metrics"

if [ -n "${ADMIN_API_TOKEN}" ]; then
  echo "Protected review metrics:"
  curl -fsS -H "X-Admin-Token: ${ADMIN_API_TOKEN}" "${API_BASE}/review/metrics"
  echo
else
  echo "Skipping authenticated review metrics. Set ADMIN_API_TOKEN to include them."
fi

echo "Launch monitor snapshot completed."
