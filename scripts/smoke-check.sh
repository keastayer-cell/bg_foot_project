#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-}"
MAILER_URL="${MAILER_URL:-}"
RETRIES="${SMOKE_RETRIES:-30}"
DELAY_SECONDS="${SMOKE_DELAY_SECONDS:-2}"

usage() {
  echo "Usage: $0 --base-url <url> [--mailer-url <url>] [--retries <count>]"
}

while (( $# > 0 )); do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --mailer-url)
      MAILER_URL="${2:-}"
      shift 2
      ;;
    --retries)
      RETRIES="${2:-}"
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

if [[ -z "$BASE_URL" ]]; then
  usage >&2
  exit 2
fi
if [[ ! "$RETRIES" =~ ^[1-9][0-9]*$ ]]; then
  echo "--retries must be a positive integer." >&2
  exit 2
fi

BASE_URL="${BASE_URL%/}"
MAILER_URL="${MAILER_URL%/}"

wait_for_up() {
  local name="$1"
  local url="$2"
  local require_up_json="$3"
  local response

  for attempt in $(seq 1 "$RETRIES"); do
    if response="$(curl --connect-timeout 3 --max-time 10 -fsS "$url" 2>/dev/null)"; then
      if [[ "$require_up_json" != "true" ]] || [[ "$response" == *'"status":"UP"'* ]] || [[ "$response" == *'"status": "UP"'* ]]; then
        echo "OK: $name"
        return 0
      fi
    fi
    printf 'Waiting for %s (%s/%s)\n' "$name" "$attempt" "$RETRIES"
    sleep "$DELAY_SECONDS"
  done

  echo "FAILED: $name ($url)" >&2
  return 1
}

wait_for_up "backend health" "$BASE_URL/api/health" true
wait_for_up "frontend home" "$BASE_URL/" false

if [[ -n "$MAILER_URL" ]]; then
  wait_for_up "mailer readiness" "$MAILER_URL/actuator/health/readiness" true
fi

echo "Post-deploy smoke-check passed."
