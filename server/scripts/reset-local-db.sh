#!/usr/bin/env bash

set -euo pipefail

# Destructive reset of the local runtime database (v_beta).
# Requires the container from start-local-db.sh to already be running.
CONTAINER_NAME="${CONTAINER_NAME:-vbeta-postgres}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5434}"
DB_NAME="${DB_NAME:-v_beta}"
SQL_USERNAME="${SQL_USERNAME:-postgres}"
SQL_PASSWORD="${SQL_PASSWORD:-postgres}"
DB_ADMIN_DB="${DB_ADMIN_DB:-postgres}"
SCHEMA_FILE="${SCHEMA_FILE:-src/main/resources/db/pg-v-beta.sql}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVER_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ "${SCHEMA_FILE}" != /* ]]; then
  SCHEMA_FILE="${SERVER_ROOT}/${SCHEMA_FILE}"
fi

if [[ "${DB_NAME}" == "v_beta_test" ]]; then
  echo "Refusing to reset test database 'v_beta_test' here. Use reset-test-db.sh." >&2
  exit 1
fi

if [[ "${DB_NAME}" != "v_beta" && "${ALLOW_NONDEFAULT_DB_RESET:-}" != "1" ]]; then
  echo "Refusing to reset unexpected database '${DB_NAME}'. Set ALLOW_NONDEFAULT_DB_RESET=1 to override." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but not installed." >&2
  exit 1
fi

if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "Schema file not found: ${SCHEMA_FILE}" >&2
  exit 1
fi

running_name="$(docker ps --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}")"
if [[ -z "${running_name}" ]]; then
  echo "Container '${CONTAINER_NAME}' is not running. Start it with ./scripts/start-local-db.sh first." >&2
  exit 1
fi

run_psql() {
  local database="$1"
  shift
  docker exec -i \
    -e "PGPASSWORD=${SQL_PASSWORD}" \
    "${CONTAINER_NAME}" \
    psql -U "${SQL_USERNAME}" -d "${database}" -v ON_ERROR_STOP=1 "$@"
}

echo "Resetting PostgreSQL runtime database '${DB_NAME}' on ${DB_HOST}:${DB_PORT}..."
run_psql "${DB_ADMIN_DB}" -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" >/dev/null
run_psql "${DB_ADMIN_DB}" -c "DROP DATABASE IF EXISTS ${DB_NAME};"
run_psql "${DB_ADMIN_DB}" -c "CREATE DATABASE ${DB_NAME};"

echo "Applying schema from ${SCHEMA_FILE}..."
run_psql "${DB_NAME}" -f - < "${SCHEMA_FILE}"

echo "PostgreSQL runtime database '${DB_NAME}' is ready."
