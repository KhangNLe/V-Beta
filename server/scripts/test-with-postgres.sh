#!/usr/bin/env bash

set -euo pipefail

TEST_DB_HOST="${TEST_DB_HOST:-${DB_HOST:-127.0.0.1}}"
TEST_DB_PORT="${TEST_DB_PORT:-55432}"
TEST_DB_NAME="${TEST_DB_NAME:-v_beta_test}"
SQL_USERNAME="${SQL_USERNAME:-postgres}"
SQL_PASSWORD="${SQL_PASSWORD:-postgres}"

# Do not export DB_NAME into Maven. Runtime shells often have DB_NAME=v_beta.
DB_HOST="${TEST_DB_HOST}" \
DB_PORT="${TEST_DB_PORT}" \
TEST_DB_HOST="${TEST_DB_HOST}" \
TEST_DB_PORT="${TEST_DB_PORT}" \
TEST_DB_NAME="${TEST_DB_NAME}" \
SQL_USERNAME="${SQL_USERNAME}" \
SQL_PASSWORD="${SQL_PASSWORD}" \
bash ./scripts/start-local-test-db.sh

echo "Running backend tests with PostgreSQL test database '${TEST_DB_NAME}'..."
TEST_DB_HOST="${TEST_DB_HOST}" \
TEST_DB_PORT="${TEST_DB_PORT}" \
TEST_DB_NAME="${TEST_DB_NAME}" \
SQL_USERNAME="${SQL_USERNAME}" \
SQL_PASSWORD="${SQL_PASSWORD}" \
./mvnw test
