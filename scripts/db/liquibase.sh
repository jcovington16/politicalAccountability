#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CHANGELOG_HOST="${ROOT_DIR}/storage-service/src/main/resources/db/changelog/db.changelog-master.xml"
CHANGELOG_DOCKER="db.changelog-master.xml"

if [ -f "${ROOT_DIR}/.env" ]; then
  set -a
  # shellcheck source=/dev/null
  source "${ROOT_DIR}/.env"
  set +a
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-political_data}"
DB_USERNAME="${DB_USERNAME:-${DATABASE_USER:-postgres}}"
DB_PASSWORD="${DB_PASSWORD:-${DATABASE_PASSWORD:-postgres}}"
DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"
LIQUIBASE_IMAGE="${LIQUIBASE_IMAGE:-liquibase:4.33}"
LIQUIBASE_DOCKER_NETWORK="${LIQUIBASE_DOCKER_NETWORK:-}"

COMMAND="${1:-status}"
shift || true

run_local() {
  liquibase \
    --changeLogFile="${CHANGELOG_HOST}" \
    --url="${DATABASE_URL}" \
    --username="${DB_USERNAME}" \
    --password="${DB_PASSWORD}" \
    "$COMMAND" "$@"
}

run_docker() {
  local docker_url="${DATABASE_URL}"
  local docker_network_arg=""

  # On Docker Desktop, localhost inside the container is the container itself.
  # The default local URL is rewritten to reach the host PostgreSQL port.
  # In CI, callers can set LIQUIBASE_DOCKER_NETWORK=host so the container can
  # reach service containers through the runner's localhost port mapping.
  if [ "${LIQUIBASE_DOCKER_NETWORK}" = "host" ]; then
    docker_network_arg="--network=host"
  elif [ "${DATABASE_URL}" = "jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}" ]; then
    docker_url="jdbc:postgresql://host.docker.internal:${DB_PORT}/${DB_NAME}"
  fi

  docker run --rm \
    ${docker_network_arg:+"${docker_network_arg}"} \
    -v "${ROOT_DIR}/storage-service/src/main/resources/db/changelog:/liquibase/changelog:ro" \
    "${LIQUIBASE_IMAGE}" \
    --searchPath="/liquibase/changelog" \
    --changeLogFile="${CHANGELOG_DOCKER}" \
    --url="${docker_url}" \
    --username="${DB_USERNAME}" \
    --password="${DB_PASSWORD}" \
    "$COMMAND" "$@"
}

if command -v liquibase >/dev/null 2>&1; then
  run_local "$@"
elif command -v docker >/dev/null 2>&1; then
  run_docker "$@"
else
  echo "Liquibase is not installed and Docker is unavailable." >&2
  echo "Install Liquibase Community CLI or Docker, then retry." >&2
  exit 1
fi
