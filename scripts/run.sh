#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! pg_isready -h 127.0.0.1 -p 5432 >/dev/null 2>&1; then
  echo "PostgreSQL no responde en 127.0.0.1:5432."
  echo "Inicia el servicio local o usa docker compose (puerto 5433) como explica el README."
  exit 1
fi

psql -h 127.0.0.1 -d postgres -f scripts/init-db.sql

if [[ -x ./mvnw ]]; then
  exec ./mvnw spring-boot:run
fi

exec mvn spring-boot:run
