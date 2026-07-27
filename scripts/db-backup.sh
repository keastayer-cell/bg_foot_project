#!/usr/bin/env bash

set -euo pipefail
umask 077

DEPLOY_ENV="${DEPLOY_ENV:-}"
OUTPUT_DIR="${BACKUP_DIR:-/var/backups/bg-foot}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

usage() {
  echo "Usage: $0 --env <test|prod> [--output-dir <path>] [--retention-days <days>]"
}

while (( $# > 0 )); do
  case "$1" in
    --env)
      DEPLOY_ENV="${2:-}"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="${2:-}"
      shift 2
      ;;
    --retention-days)
      RETENTION_DAYS="${2:-}"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ "$DEPLOY_ENV" != "test" && "$DEPLOY_ENV" != "prod" ]]; then
  echo "--env must be test or prod." >&2
  exit 2
fi
if [[ ! "$RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]]; then
  echo "--retention-days must be a positive integer." >&2
  exit 2
fi

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

command -v pg_dump >/dev/null || { echo "pg_dump is required." >&2; exit 1; }

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$OUTPUT_DIR/$DEPLOY_ENV"
backup_path="$OUTPUT_DIR/$DEPLOY_ENV/${DB_NAME}_${timestamp}.dump"
temporary_path="${backup_path}.partial"

cleanup() {
  rm -f "$temporary_path"
}
trap cleanup EXIT

PGPASSWORD="$DB_PASSWORD" pg_dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --format=custom \
  --compress=9 \
  --no-owner \
  --no-privileges \
  --file="$temporary_path"

mv "$temporary_path" "$backup_path"
if command -v sha256sum >/dev/null 2>&1; then
  (
    cd "$(dirname "$backup_path")"
    sha256sum "$(basename "$backup_path")" >"$(basename "$backup_path").sha256"
  )
else
  (
    cd "$(dirname "$backup_path")"
    shasum -a 256 "$(basename "$backup_path")" >"$(basename "$backup_path").sha256"
  )
fi

find "$OUTPUT_DIR/$DEPLOY_ENV" -type f \
  \( -name '*.dump' -o -name '*.dump.sha256' \) \
  -mtime "+$RETENTION_DAYS" -delete

echo "$backup_path"
