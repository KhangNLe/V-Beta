#!/usr/bin/env bash

set -euo pipefail

# Local runtime PostgreSQL for the app (v_beta). Separate from the test DB
# container (vbeta-test-postgres on 55432).
CONTAINER_NAME="${CONTAINER_NAME:-vbeta-postgres}"
VOLUME_NAME="${VOLUME_NAME:-vbeta-postgres-data}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16}"
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
  echo "Refusing to manage test database 'v_beta_test' here. Use start-local-test-db.sh." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but not installed." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not reachable." >&2
  echo "Start Docker Desktop and wait until it is running, then retry." >&2
  echo "On Windows you can also run: powershell -File ./scripts/start-local-db.ps1" >&2
  exit 1
fi

if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "Schema file not found: ${SCHEMA_FILE}" >&2
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

container_exists="$(docker ps -a --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}")"
if [[ -z "${container_exists}" ]]; then
  echo "Creating PostgreSQL container ${CONTAINER_NAME} on port ${DB_PORT}..."
  docker volume create "${VOLUME_NAME}" >/dev/null
  docker run -d \
    --name "${CONTAINER_NAME}" \
    -e POSTGRES_USER="${SQL_USERNAME}" \
    -e POSTGRES_PASSWORD="${SQL_PASSWORD}" \
    -e POSTGRES_DB="${DB_ADMIN_DB}" \
    -p "${DB_PORT}:5432" \
    -v "${VOLUME_NAME}:/var/lib/postgresql/data" \
    "${POSTGRES_IMAGE}" >/dev/null
else
  running_name="$(docker ps --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}")"
  if [[ -z "${running_name}" ]]; then
    echo "Starting existing PostgreSQL container ${CONTAINER_NAME}..."
    docker start "${CONTAINER_NAME}" >/dev/null
  fi
fi

echo "Waiting for PostgreSQL readiness..."
for _ in $(seq 1 30); do
  if docker exec "${CONTAINER_NAME}" pg_isready -U "${SQL_USERNAME}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! docker exec "${CONTAINER_NAME}" pg_isready -U "${SQL_USERNAME}" >/dev/null 2>&1; then
  echo "PostgreSQL container did not become ready in time." >&2
  docker logs --tail 40 "${CONTAINER_NAME}" >&2 || true
  exit 1
fi

db_exists="$(run_psql "${DB_ADMIN_DB}" -Atc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}';" | tr -d '[:space:]')"
if [[ "${db_exists}" != "1" ]]; then
  echo "Creating database '${DB_NAME}'..."
  run_psql "${DB_ADMIN_DB}" -c "CREATE DATABASE ${DB_NAME};"
fi

schema_ready="$(run_psql "${DB_NAME}" -Atc "SELECT to_regclass('public.gym_role');" | tr -d '[:space:]')"
if [[ -z "${schema_ready}" || "${schema_ready}" == "null" ]]; then
  echo "Applying schema from ${SCHEMA_FILE}..."
  run_psql "${DB_NAME}" -f - < "${SCHEMA_FILE}"
else
  echo "Schema already present in '${DB_NAME}'; skipping apply."
fi

echo "Local PostgreSQL runtime DB is ready on ${DB_HOST}:${DB_PORT}/${DB_NAME}."
echo "Match server/.env: DB_HOST=${DB_HOST} DB_PORT=${DB_PORT} DB_NAME=${DB_NAME} SQL_USERNAME=${SQL_USERNAME}"
