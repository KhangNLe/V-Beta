#!/usr/bin/env bash

set -euo pipefail

# Isolated test database. Never read DB_NAME — that is the runtime app database
# and is often exported in the same shell as `./mvnw spring-boot:run`.
DB_HOST="${TEST_DB_HOST:-${DB_HOST:-127.0.0.1}}"
DB_PORT="${TEST_DB_PORT:-${DB_PORT:-5432}}"
TEST_DB_NAME="${TEST_DB_NAME:-v_beta_test}"
SQL_USERNAME="${SQL_USERNAME:-postgres}"
SQL_PASSWORD="${SQL_PASSWORD:-postgres}"
DB_ADMIN_DB="${DB_ADMIN_DB:-postgres}"
SCHEMA_FILE="${SCHEMA_FILE:-src/test/resources/db/v_beta_test_schema.sql}"

if [[ "${TEST_DB_NAME}" == "v_beta" ]]; then
  echo "Refusing to reset runtime database 'v_beta'. Set TEST_DB_NAME=v_beta_test." >&2
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is required but not installed." >&2
  exit 1
fi

if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "Schema file not found: ${SCHEMA_FILE}" >&2
  exit 1
fi

export PGPASSWORD="${SQL_PASSWORD}"
PSQL_BASE=(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${SQL_USERNAME}" -v ON_ERROR_STOP=1)

echo "Resetting PostgreSQL test database '${TEST_DB_NAME}' on ${DB_HOST}:${DB_PORT}..."
"${PSQL_BASE[@]}" -d "${DB_ADMIN_DB}" -c "DROP DATABASE IF EXISTS ${TEST_DB_NAME};"
"${PSQL_BASE[@]}" -d "${DB_ADMIN_DB}" -c "CREATE DATABASE ${TEST_DB_NAME};"

echo "Applying schema and seed data from ${SCHEMA_FILE}..."
"${PSQL_BASE[@]}" -d "${TEST_DB_NAME}" -f "${SCHEMA_FILE}"

echo "PostgreSQL test database '${TEST_DB_NAME}' is ready."
