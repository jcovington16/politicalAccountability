#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHANGES_DIR="${ROOT_DIR}/storage-service/src/main/resources/db/changelog/changes"

raw_name="${1:-}"
if [ -z "${raw_name}" ]; then
  echo "Usage: scripts/db/new-migration.sh add_short_description" >&2
  exit 1
fi

safe_name="$(echo "${raw_name}" | tr '[:upper:]' '[:lower:]' | sed -E 's/[^a-z0-9]+/_/g; s/^_+|_+$//g')"
timestamp="$(date -u +"%Y%m%d%H%M%S")"
file="${CHANGES_DIR}/${timestamp}_${safe_name}.sql"

mkdir -p "${CHANGES_DIR}"

cat > "${file}" <<EOF
--liquibase formatted sql

--changeset joshua:${timestamp}-${safe_name}
--comment: Describe the database change here.
-- Add SQL here.

--rollback Add rollback SQL here.
EOF

echo "${file}"
