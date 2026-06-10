#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
REQUESTS="${REQUESTS:-30}"
QUERY="${QUERY:-Trump}"

echo "Running ${REQUESTS} search requests against ${API_BASE}/search?query=${QUERY}"

start_seconds="$(date +%s)"
ok=0
rate_limited=0
client_error=0

for i in $(seq 1 "${REQUESTS}"); do
  status="$(curl -sS -o /dev/null -w '%{http_code}' "${API_BASE}/search?query=${QUERY}&limit=5")"
  case "${status}" in
    200) ok=$((ok + 1)) ;;
    429) rate_limited=$((rate_limited + 1)) ;;
    400|404) client_error=$((client_error + 1)) ;;
    *) echo "Unexpected status ${status} on request ${i}" >&2; exit 1 ;;
  esac
done

end_seconds="$(date +%s)"
elapsed=$((end_seconds - start_seconds))

echo "Completed: ok=${ok} rate_limited=${rate_limited} client_error=${client_error} elapsed_seconds=${elapsed}"
