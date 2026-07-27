#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_ENV=""
ENV_ROOT="${ENV_ROOT:-/etc/bg-foot}"

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

if (( EUID != 0 )); then
  echo "Run this script as root." >&2
  exit 1
fi
if [[ "$DEPLOY_ENV" != "test" && "$DEPLOY_ENV" != "prod" ]]; then
  echo "--env must be test or prod." >&2
  exit 2
fi

target_dir="$ENV_ROOT/$DEPLOY_ENV"
install -d -m 0700 -o root -g root "$target_dir"

for env_file in common app mailer; do
  target="$target_dir/$env_file.env"
  if [[ -e "$target" ]]; then
    echo "Keeping existing $target"
    continue
  fi
  install -m 0600 -o root -g root \
    "$REPO_ROOT/ops/env/$env_file.env.example" \
    "$target"
  echo "Created template $target"
done

if [[ "$DEPLOY_ENV" == "prod" ]]; then
  sed -i 's/^APP_PROFILE=test$/APP_PROFILE=prod/' "$target_dir/app.env"
fi

echo "Replace every placeholder in $target_dir before enabling services."
