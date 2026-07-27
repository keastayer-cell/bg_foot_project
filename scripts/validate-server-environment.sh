#!/usr/bin/env bash

set -euo pipefail

DEPLOY_ENV=""
ENV_ROOT="${ENV_ROOT:-/etc/bg-foot}"
EXPECTED_ENV_OWNER="${EXPECTED_ENV_OWNER:-root}"

usage() {
  echo "Usage: $0 --env <test|prod>"
}

while (( $# > 0 )); do
  case "$1" in
    --env)
      DEPLOY_ENV="${2:-}"
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

env_dir="$ENV_ROOT/$DEPLOY_ENV"

file_permissions() {
  stat -c '%a' "$1" 2>/dev/null || stat -f '%Lp' "$1"
}

file_owner() {
  stat -c '%U' "$1" 2>/dev/null || stat -f '%Su' "$1"
}

for env_file in common.env app.env mailer.env; do
  path="$env_dir/$env_file"
  [[ -f "$path" ]] || { echo "Missing $path" >&2; exit 1; }
  permissions="$(file_permissions "$path")"
  [[ "$permissions" == "600" ]] || { echo "$path must have mode 600, found $permissions" >&2; exit 1; }
  [[ "$(file_owner "$path")" == "$EXPECTED_ENV_OWNER" ]] || {
    echo "$path must be owned by $EXPECTED_ENV_OWNER." >&2
    exit 1
  }
  if grep -Eqi '(^|=).*(replace-with|change-me|example\.(com|invalid))' "$path"; then
    echo "$path still contains placeholder values." >&2
    exit 1
  fi
done

required_common=(DB_HOST DB_PORT DB_NAME DB_SCHEMA DB_USER DB_PASSWORD)
required_app=(APP_PROFILE JWT_SECRET)
required_mailer=(MAILER_TRANSPORT_TYPE MAILER_FROM_EMAIL)

check_required() {
  local path="$1"
  shift
  local key
  for key in "$@"; do
    grep -Eq "^${key}=.+" "$path" || { echo "$path is missing $key." >&2; exit 1; }
  done
}

check_required "$env_dir/common.env" "${required_common[@]}"
check_required "$env_dir/app.env" "${required_app[@]}"
check_required "$env_dir/mailer.env" "${required_mailer[@]}"

if ! grep -Eq "^APP_PROFILE=${DEPLOY_ENV}$" "$env_dir/app.env"; then
  echo "APP_PROFILE must match $DEPLOY_ENV." >&2
  exit 1
fi
if grep -Eq '^MAILER_TRANSPORT_TYPE=smtp$' "$env_dir/mailer.env"; then
  check_required "$env_dir/mailer.env" MAILER_SMTP_USERNAME MAILER_SMTP_PASSWORD
fi

echo "Server environment '$DEPLOY_ENV' is complete and has safe permissions."
