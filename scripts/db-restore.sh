#!/usr/bin/env bash

set -euo pipefail

DEPLOY_ENV="${DEPLOY_ENV:-}"
BACKUP_PATH=""
CONFIRMED_ENV=""
ASSUME_YES=false

usage() {
  echo "Usage: $0 --env <test|prod> --backup <file.dump> --confirm-env <test|prod> --yes"
}

while (( $# > 0 )); do
  case "$1" in
    --env)
      DEPLOY_ENV="${2:-}"
      shift 2
      ;;
    --backup)
      BACKUP_PATH="${2:-}"
      shift 2
      ;;
    --confirm-env)
      CONFIRMED_ENV="${2:-}"
      shift 2
      ;;
    --yes)
      ASSUME_YES=true
      shift
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
if [[ "$CONFIRMED_ENV" != "$DEPLOY_ENV" || "$ASSUME_YES" != "true" ]]; then
  echo "Restore requires matching --confirm-env and --yes." >&2
  exit 2
fi
if [[ "$DEPLOY_ENV" == "prod" && "${ALLOW_PRODUCTION_RESTORE:-}" != "yes" ]]; then
  echo "Production restore additionally requires ALLOW_PRODUCTION_RESTORE=yes." >&2
  exit 2
fi
if [[ ! -f "$BACKUP_PATH" ]]; then
  echo "Backup does not exist: $BACKUP_PATH" >&2
  exit 1
fi

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PORT:?DB_PORT is required}"
: "${DB_NAME:?DB_NAME is required}"
: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

command -v pg_restore >/dev/null || { echo "pg_restore is required." >&2; exit 1; }

checksum_path="${BACKUP_PATH}.sha256"
if [[ -f "$checksum_path" ]]; then
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$(dirname "$BACKUP_PATH")" && sha256sum --check "$(basename "$checksum_path")")
  else
    expected_checksum="$(awk '{print $1}' "$checksum_path")"
    actual_checksum="$(shasum -a 256 "$BACKUP_PATH" | awk '{print $1}')"
    [[ "$expected_checksum" == "$actual_checksum" ]] || { echo "Backup checksum mismatch." >&2; exit 1; }
  fi
else
  echo "Checksum file not found: $checksum_path" >&2
  exit 1
fi

echo "Restoring $DEPLOY_ENV database '$DB_NAME'. Existing objects will be replaced."
PGPASSWORD="$DB_PASSWORD" pg_restore \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  --exit-on-error \
  "$BACKUP_PATH"

echo "Database restore completed."
