#!/usr/bin/env bash

set -euo pipefail

RELEASE_DIR="${1:-}"
RELEASE_ID="${RELEASE_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
DEPLOY_ENV="${DEPLOY_ENV:-test}"
APP_JAR_PATH="${APP_JAR_PATH:-/opt/football-stats-app/app.jar}"
MAILER_JAR_PATH="${MAILER_JAR_PATH:-/opt/football-stats-mailer/mailer.jar}"
WEB_ROOT="${WEB_ROOT:-/var/www/football-stats-web}"
ENV_ROOT="${ENV_ROOT:-/etc/bg-foot}"
ENV_DIR="$ENV_ROOT/$DEPLOY_ENV"
COMMON_ENV_FILE="$ENV_DIR/common.env"
RELEASES_ROOT="${RELEASES_ROOT:-/opt/bg-foot-releases}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/bg-foot}"
APP_SERVICE="${APP_SERVICE:-football-stats-app@$DEPLOY_ENV}"
MAILER_SERVICE="${MAILER_SERVICE:-football-stats-mailer@$DEPLOY_ENV}"
LOCAL_BASE_URL="${LOCAL_BASE_URL:-http://127.0.0.1}"
LOCAL_MAILER_URL="${LOCAL_MAILER_URL:-http://127.0.0.1:8090}"

if (( EUID != 0 )); then
  echo "Remote deploy must run as root." >&2
  exit 1
fi
if [[ -z "$RELEASE_DIR" || ! -d "$RELEASE_DIR" ]]; then
  echo "Usage: $0 <release-directory>" >&2
  exit 2
fi

for artifact in app.jar mailer.jar web-dist.tgz smoke-check.sh db-backup.sh validate-server-environment.sh; do
  if [[ ! -f "$RELEASE_DIR/$artifact" ]]; then
    echo "Missing release artifact: $artifact" >&2
    exit 1
  fi
done
for policy in \
  ops/systemd/football-stats-app@.service \
  ops/systemd/football-stats-mailer@.service \
  ops/journald/60-bg-foot.conf \
  ops/logrotate/bg-foot-nginx; do
  if [[ ! -f "$RELEASE_DIR/$policy" ]]; then
    echo "Missing operational policy: $policy" >&2
    exit 1
  fi
done
for env_file in common.env app.env mailer.env; do
  [[ -f "$ENV_DIR/$env_file" ]] || { echo "Missing server environment file: $ENV_DIR/$env_file" >&2; exit 1; }
done

ENV_ROOT="$ENV_ROOT" bash "$RELEASE_DIR/validate-server-environment.sh" --env "$DEPLOY_ENV"

exec 9>/var/lock/bg-foot-deploy.lock
if ! flock -n 9; then
  echo "Another deployment is already running." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$COMMON_ENV_FILE"
set +a

release_state="$RELEASES_ROOT/$RELEASE_ID"
previous_state="$release_state/previous"
mkdir -p "$previous_state"

deployment_started=false

rollback_runtime() {
  local exit_code=$?
  trap - ERR
  if [[ "$deployment_started" != "true" ]]; then
    exit "$exit_code"
  fi

  echo "Deployment failed. Restoring previous application artifacts." >&2
  if [[ -f "$previous_state/app.jar" ]]; then
    cp -p "$previous_state/app.jar" "$APP_JAR_PATH"
  fi
  if [[ -f "$previous_state/mailer.jar" ]]; then
    cp -p "$previous_state/mailer.jar" "$MAILER_JAR_PATH"
  fi
  if [[ -f "$previous_state/web-dist.tgz" ]]; then
    rm -rf "${WEB_ROOT:?}/"*
    tar -xzf "$previous_state/web-dist.tgz" -C "$WEB_ROOT"
  fi

  systemctl restart "$APP_SERVICE" || true
  systemctl restart "$MAILER_SERVICE" || true
  echo "Runtime rollback completed. Database migrations were not reverted." >&2
  echo "Database backup: ${database_backup_path:-not-created}" >&2
  exit "$exit_code"
}
trap rollback_runtime ERR

echo "== database backup =="
database_backup_path="$(
  BACKUP_DIR="$BACKUP_DIR" \
  bash "$RELEASE_DIR/db-backup.sh" --env "$DEPLOY_ENV"
)"
echo "Database backup created: $database_backup_path"

if [[ -f "$APP_JAR_PATH" ]]; then
  cp -p "$APP_JAR_PATH" "$previous_state/app.jar"
fi
if [[ -f "$MAILER_JAR_PATH" ]]; then
  cp -p "$MAILER_JAR_PATH" "$previous_state/mailer.jar"
fi
mkdir -p "$WEB_ROOT"
tar -C "$WEB_ROOT" -czf "$previous_state/web-dist.tgz" .
printf '%s\n' \
  "release=$RELEASE_ID" \
  "deployed_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  "database_backup=$database_backup_path" \
  >"$release_state/manifest"

install -D -m 0644 \
  "$RELEASE_DIR/ops/systemd/football-stats-app@.service" \
  /etc/systemd/system/football-stats-app@.service
install -D -m 0644 \
  "$RELEASE_DIR/ops/systemd/football-stats-mailer@.service" \
  /etc/systemd/system/football-stats-mailer@.service
install -D -m 0644 \
  "$RELEASE_DIR/ops/journald/60-bg-foot.conf" \
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
    "$RELEASE_DIR/ops/logrotate/bg-foot-nginx" \
    /etc/logrotate.d/bg-foot-nginx
  nginx_logrotate_policy=/etc/logrotate.d/bg-foot-nginx
fi

systemctl daemon-reload
systemctl try-reload-or-restart systemd-journald.service
logrotate --debug "$nginx_logrotate_policy" >/dev/null

deployment_started=true
getent group bg-foot >/dev/null || groupadd --system bg-foot
id bg-foot >/dev/null 2>&1 || useradd --system --gid bg-foot --home-dir /nonexistent --shell /usr/sbin/nologin bg-foot
install -d -m 0750 -o root -g bg-foot "$(dirname "$APP_JAR_PATH")" "$(dirname "$MAILER_JAR_PATH")"
install -D -m 0640 -o root -g bg-foot "$RELEASE_DIR/app.jar" "$APP_JAR_PATH"
install -D -m 0640 -o root -g bg-foot "$RELEASE_DIR/mailer.jar" "$MAILER_JAR_PATH"

web_staging="${WEB_ROOT}.next-${RELEASE_ID}"
rm -rf "$web_staging"
mkdir -p "$web_staging"
tar -xzf "$RELEASE_DIR/web-dist.tgz" -C "$web_staging"
rm -rf "${WEB_ROOT:?}/"*
cp -a "$web_staging/." "$WEB_ROOT/"
rm -rf "$web_staging"

nginx -t
systemctl restart "$APP_SERVICE"
systemctl restart "$MAILER_SERVICE"
systemctl enable "$APP_SERVICE" "$MAILER_SERVICE"
systemctl reload nginx

bash "$RELEASE_DIR/smoke-check.sh" \
  --base-url "$LOCAL_BASE_URL" \
  --mailer-url "$LOCAL_MAILER_URL" \
  --retries 45

deployment_started=false
find "$RELEASES_ROOT" -mindepth 1 -maxdepth 1 -type d -mtime +30 -exec rm -rf {} +
echo "Test deployment $RELEASE_ID completed."
echo "Runtime rollback source: $previous_state"
echo "Database backup: $database_backup_path"
