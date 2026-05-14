#!/usr/bin/env bash

set -euo pipefail

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-55432}"
DB_NAME="${DB_NAME:-v_beta_test}"
SQL_USERNAME="${SQL_USERNAME:-postgres}"
SQL_PASSWORD="${SQL_PASSWORD:-postgres}"

bash ./scripts/start-local-test-db.sh

echo "Running backend tests with PostgreSQL..."
DB_HOST="${DB_HOST}" \
DB_PORT="${DB_PORT}" \
DB_NAME="${DB_NAME}" \
SQL_USERNAME="${SQL_USERNAME}" \
SQL_PASSWORD="${SQL_PASSWORD}" \
./mvnw test
