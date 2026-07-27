#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if (( EUID != 0 )); then
  echo "Run this script as root." >&2
  exit 1
fi

install -m 0755 "$REPO_ROOT/scripts/db-backup.sh" /usr/local/sbin/bg-foot-db-backup
install -m 0755 "$REPO_ROOT/scripts/db-restore.sh" /usr/local/sbin/bg-foot-db-restore
install -m 0755 "$REPO_ROOT/scripts/smoke-check.sh" /usr/local/sbin/bg-foot-smoke-check
install -m 0644 \
  "$REPO_ROOT/ops/systemd/bg-foot-db-backup@.service" \
  /etc/systemd/system/bg-foot-db-backup@.service
install -m 0644 \
  "$REPO_ROOT/ops/systemd/bg-foot-db-backup@.timer" \
  /etc/systemd/system/bg-foot-db-backup@.timer

mkdir -p /etc/bg-foot
chmod 0700 /etc/bg-foot
systemctl daemon-reload

enabled_count=0
for deploy_env in test prod; do
  if [[ -f "/etc/bg-foot/$deploy_env.env" ]]; then
    chmod 0600 "/etc/bg-foot/$deploy_env.env"
    systemctl enable --now "bg-foot-db-backup@$deploy_env.timer"
    enabled_count=$((enabled_count + 1))
  else
    echo "Skipping $deploy_env timer: /etc/bg-foot/$deploy_env.env does not exist."
  fi
done

echo "Backup tools installed; enabled $enabled_count timer(s)."
