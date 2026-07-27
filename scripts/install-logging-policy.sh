#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if (( EUID != 0 )); then
  echo "Run this script as root." >&2
  exit 1
fi

install -D -m 0644 \
  "$REPO_ROOT/ops/journald/60-bg-foot.conf" \
  /etc/systemd/journald.conf.d/60-bg-foot.conf

existing_nginx_policy="$(
  grep -Rsl '/var/log/nginx/access.log' /etc/logrotate.d 2>/dev/null \
    | grep -v '^/etc/logrotate.d/bg-foot-nginx$' \
    | head -n 1 \
    || true
)"
if [[ -n "$existing_nginx_policy" ]]; then
  rm -f /etc/logrotate.d/bg-foot-nginx
  nginx_logrotate_policy="$existing_nginx_policy"
  echo "Keeping existing Nginx logrotate policy: $existing_nginx_policy"
else
  install -m 0644 \
    "$REPO_ROOT/ops/logrotate/bg-foot-nginx" \
    /etc/logrotate.d/bg-foot-nginx
  nginx_logrotate_policy=/etc/logrotate.d/bg-foot-nginx
fi

systemctl daemon-reload
systemctl try-reload-or-restart systemd-journald.service
logrotate --debug "$nginx_logrotate_policy" >/dev/null

echo "Logging policy installed. Restart application services during the next deploy."
