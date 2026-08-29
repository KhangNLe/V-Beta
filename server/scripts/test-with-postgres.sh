#!/usr/bin/env bash

set -euo pipefail

TEST_DB_HOST="${TEST_DB_HOST:-127.0.0.1}"
TEST_DB_PORT="${TEST_DB_PORT:-55432}"
TEST_DB_NAME="${TEST_DB_NAME:-v_beta_test}"
TEST_SQL_USERNAME="${TEST_SQL_USERNAME:-postgres}"
TEST_SQL_PASSWORD="${TEST_SQL_PASSWORD:-postgres}"

# Do not export DB_NAME / SQL_* into Maven or reset. Runtime shells often
# have DB_NAME=v_beta and SQL_* from server/.env.
TEST_DB_HOST="${TEST_DB_HOST}" \
TEST_DB_PORT="${TEST_DB_PORT}" \
TEST_DB_NAME="${TEST_DB_NAME}" \
TEST_SQL_USERNAME="${TEST_SQL_USERNAME}" \
TEST_SQL_PASSWORD="${TEST_SQL_PASSWORD}" \
bash ./scripts/start-local-test-db.sh

echo "Running backend tests with PostgreSQL test database '${TEST_DB_NAME}'..."
TEST_DB_HOST="${TEST_DB_HOST}" \
TEST_DB_PORT="${TEST_DB_PORT}" \
TEST_DB_NAME="${TEST_DB_NAME}" \
TEST_SQL_USERNAME="${TEST_SQL_USERNAME}" \
TEST_SQL_PASSWORD="${TEST_SQL_PASSWORD}" \
./mvnw test
