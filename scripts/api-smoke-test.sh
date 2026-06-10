#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
ADMIN_API_TOKEN="${ADMIN_API_TOKEN:-local-admin-token}"

echo "Smoke testing ${API_BASE}"

search_body="$(curl -fsS "${API_BASE}/search?query=Trump&limit=3")"
echo "${search_body}" | grep -q '"groups"'

bill_body="$(curl -fsS "${API_BASE}/bills/search?query=health&limit=3")"
echo "${bill_body}" | grep -q '\['

claims_body="$(curl -fsS "${API_BASE}/claims?query=health&limit=3")"
echo "${claims_body}" | grep -q '\['

citations_body="$(curl -fsS "${API_BASE}/citations?query=Congress&limit=3")"
echo "${citations_body}" | grep -q '\['

sources_body="$(curl -fsS "${API_BASE}/sources?query=Congress&limit=3")"
echo "${sources_body}" | grep -q '\['

trust_body="$(curl -fsS -H "Content-Type: application/json" -d '{"informationType":"VOTING_RECORD","sourceQuality":"OFFICIAL_RECORD","citationCount":2}' "${API_BASE}/trust/score")"
echo "${trust_body}" | grep -q '"score"'

metrics_status="$(curl -sS -o /dev/null -w '%{http_code}' "${API_BASE}/review/metrics")"
test "${metrics_status}" = "401"

metrics_body="$(curl -fsS -H "X-Admin-Token: ${ADMIN_API_TOKEN}" "${API_BASE}/review/metrics")"
echo "${metrics_body}" | grep -q '"statusCounts"'

echo "API smoke test passed"
