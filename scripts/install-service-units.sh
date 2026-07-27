#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPLOY_ENV=""

while (( $# > 0 )); do
  case "$1" in
    --env)
      DEPLOY_ENV="${2:-}"
      shift 2
      ;;
    --help|-h)
      echo "Usage: $0 --env <test|prod>"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

if (( EUID != 0 )); then
  echo "Run this script as root." >&2
  exit 1
fi

bash "$REPO_ROOT/scripts/validate-server-environment.sh" --env "$DEPLOY_ENV"

getent group bg-foot >/dev/null || groupadd --system bg-foot
id bg-foot >/dev/null 2>&1 || useradd --system --gid bg-foot --home-dir /nonexistent --shell /usr/sbin/nologin bg-foot
install -d -m 0750 -o root -g bg-foot /opt/football-stats-app /opt/football-stats-mailer
install -m 0644 "$REPO_ROOT/ops/systemd/football-stats-app@.service" /etc/systemd/system/
install -m 0644 "$REPO_ROOT/ops/systemd/football-stats-mailer@.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable "football-stats-app@$DEPLOY_ENV.service" "football-stats-mailer@$DEPLOY_ENV.service"

echo "Service units installed for $DEPLOY_ENV. They were not started."
