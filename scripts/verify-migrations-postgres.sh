#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_JAR="${APP_JAR:-$REPO_ROOT/app/target/football-stats-app-0.0.1-SNAPSHOT.jar}"
APP_PORT="${APP_PORT:-18080}"
LOG_FILE="${MIGRATION_VERIFY_LOG:-${TMPDIR:-/tmp}/bg-foot-migration-verify.log}"

if [[ ! -f "$APP_JAR" ]]; then
  echo "Backend jar not found: $APP_JAR" >&2
  exit 1
fi

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
: "${JWT_SECRET:?JWT_SECRET is required}"

cleanup() {
  if [[ -n "${app_pid:-}" ]]; then
    kill "$app_pid" >/dev/null 2>&1 || true
    wait "$app_pid" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

APP_PROFILE=test \
APP_PORT="$APP_PORT" \
MAILER_TRIGGER_ENABLED=false \
java -jar "$APP_JAR" >"$LOG_FILE" 2>&1 &
app_pid=$!

for attempt in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:$APP_PORT/api/health" >/dev/null; then
    echo "Flyway migrations applied and backend health is UP."
    exit 0
  fi
  if ! kill -0 "$app_pid" >/dev/null 2>&1; then
    echo "Backend exited while applying migrations." >&2
    tail -n 160 "$LOG_FILE" >&2
    exit 1
  fi
  sleep 1
done

echo "Backend did not become healthy after migration validation." >&2
tail -n 160 "$LOG_FILE" >&2
exit 1
