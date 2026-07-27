#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEST_ROOT="$(mktemp -d)"

cleanup() {
  rm -rf "$TEST_ROOT"
}
trap cleanup EXIT

mkdir -p "$TEST_ROOT/bin" "$TEST_ROOT/backups"

cat >"$TEST_ROOT/bin/pg_dump" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
for argument in "$@"; do
  case "$argument" in
    --file=*)
      printf 'fake-postgres-dump\n' >"${argument#--file=}"
      ;;
  esac
done
EOF

cat >"$TEST_ROOT/bin/pg_restore" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' "$@" >"${FAKE_RESTORE_ARGS:?}"
EOF

cat >"$TEST_ROOT/bin/curl" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
url="${*: -1}"
case "$url" in
  */api/health|*/actuator/health/readiness)
    printf '{"status":"UP"}'
    ;;
  */)
    printf '<!doctype html><title>BG Foot</title>'
    ;;
  *)
    exit 22
    ;;
esac
EOF

chmod +x "$TEST_ROOT/bin/pg_dump" "$TEST_ROOT/bin/pg_restore" "$TEST_ROOT/bin/curl"

common_env=(
  "PATH=$TEST_ROOT/bin:$PATH"
  "DB_HOST=127.0.0.1"
  "DB_PORT=5432"
  "DB_NAME=football_test"
  "DB_USER=football_test"
  "DB_PASSWORD=not-printed"
)

backup_path="$(
  env "${common_env[@]}" \
    bash "$REPO_ROOT/scripts/db-backup.sh" \
      --env test \
      --output-dir "$TEST_ROOT/backups" \
      --retention-days 2
)"

[[ -f "$backup_path" ]]
[[ -f "${backup_path}.sha256" ]]

env "${common_env[@]}" \
  FAKE_RESTORE_ARGS="$TEST_ROOT/restore-args" \
  bash "$REPO_ROOT/scripts/db-restore.sh" \
    --env test \
    --backup "$backup_path" \
    --confirm-env test \
    --yes

grep -q -- '--clean' "$TEST_ROOT/restore-args"
grep -q -- '--exit-on-error' "$TEST_ROOT/restore-args"

if env "${common_env[@]}" \
  FAKE_RESTORE_ARGS="$TEST_ROOT/prod-restore-args" \
  bash "$REPO_ROOT/scripts/db-restore.sh" \
    --env prod \
    --backup "$backup_path" \
    --confirm-env prod \
    --yes >/dev/null 2>&1; then
  echo "Production restore unexpectedly passed without ALLOW_PRODUCTION_RESTORE=yes." >&2
  exit 1
fi

PATH="$TEST_ROOT/bin:$PATH" \
  SMOKE_DELAY_SECONDS=0 \
  bash "$REPO_ROOT/scripts/smoke-check.sh" \
    --base-url http://bg-foot.test \
    --mailer-url http://mailer.test \
    --retries 1

echo "Operational script tests passed."
