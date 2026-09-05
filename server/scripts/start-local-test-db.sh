#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-vbeta-test-postgres}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16}"
DB_HOST="${TEST_DB_HOST:-127.0.0.1}"
DB_PORT="${TEST_DB_PORT:-55432}"
TEST_DB_NAME="${TEST_DB_NAME:-v_beta_test}"
SQL_USERNAME="${TEST_SQL_USERNAME:-postgres}"
SQL_PASSWORD="${TEST_SQL_PASSWORD:-postgres}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but not installed." >&2
  exit 1
fi

container_exists="$(docker ps -a --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}")"
if [[ -z "${container_exists}" ]]; then
  echo "Creating PostgreSQL container ${CONTAINER_NAME}..."
  docker run -d \
    --name "${CONTAINER_NAME}" \
    -e POSTGRES_USER="${SQL_USERNAME}" \
    -e POSTGRES_PASSWORD="${SQL_PASSWORD}" \
    -e POSTGRES_DB=postgres \
    -p "${DB_PORT}:5432" \
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
  exit 1
fi

TEST_DB_HOST="${DB_HOST}" \
TEST_DB_PORT="${DB_PORT}" \
TEST_DB_NAME="${TEST_DB_NAME}" \
TEST_SQL_USERNAME="${SQL_USERNAME}" \
TEST_SQL_PASSWORD="${SQL_PASSWORD}" \
bash ./scripts/reset-test-db.sh

echo "Local PostgreSQL test DB is ready on ${DB_HOST}:${DB_PORT}."
