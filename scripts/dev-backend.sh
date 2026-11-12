#!/usr/bin/env bash
# Starts the reservation service locally against a compose-managed PostgreSQL.
# Flyway applies the schema at startup. Requires Docker.
#
# Usage: scripts/dev-backend.sh
# Stop:  docker compose down  (add -v to drop the data volume)
set -euo pipefail

cd "$(dirname "$0")/.."

LILSERVE_DATASOURCE_URL="${LILSERVE_DATASOURCE_URL:-jdbc:postgresql://localhost:5433/lilserve}"
LILSERVE_DATASOURCE_USERNAME="${LILSERVE_DATASOURCE_USERNAME:-lilserve}"
LILSERVE_DATASOURCE_PASSWORD="${LILSERVE_DATASOURCE_PASSWORD:-lilserve}"

docker compose up -d

echo "OpenAPI:  http://localhost:8080/v3/api-docs"
echo "Swagger:  http://localhost:8080/swagger-ui/index.html"

exec env \
    LILSERVE_DATASOURCE_URL="$LILSERVE_DATASOURCE_URL" \
    LILSERVE_DATASOURCE_USERNAME="$LILSERVE_DATASOURCE_USERNAME" \
    LILSERVE_DATASOURCE_PASSWORD="$LILSERVE_DATASOURCE_PASSWORD" \
    ./gradlew :reservation-service:launcher:bootRun --console=plain
