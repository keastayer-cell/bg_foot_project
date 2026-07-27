#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MIGRATION_DIR="${1:-$REPO_ROOT/app/src/main/resources/db/migration}"

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "Migration directory does not exist: $MIGRATION_DIR" >&2
  exit 1
fi

expected_version=1
migration_count=0

while IFS= read -r migration; do
  filename="$(basename "$migration")"
  if [[ ! "$filename" =~ ^V([0-9]+)__([a-z0-9][a-z0-9_]*)\.sql$ ]]; then
    echo "Invalid migration filename: $filename" >&2
    exit 1
  fi

  version=$((10#${BASH_REMATCH[1]}))
  if (( version != expected_version )); then
    echo "Migration sequence gap: expected V$expected_version, found V$version ($filename)" >&2
    exit 1
  fi
  if [[ ! -s "$migration" ]]; then
    echo "Migration is empty: $filename" >&2
    exit 1
  fi

  expected_version=$((expected_version + 1))
  migration_count=$((migration_count + 1))
done < <(
  find "$MIGRATION_DIR" -maxdepth 1 -type f -name 'V*.sql' \
    | awk -F/ '{
        name = $NF
        separator = index(name, "__")
        version = separator > 2 ? substr(name, 2, separator - 2) : 0
        print version "\t" $0
      }' \
    | sort -n -k1,1 \
    | cut -f2-
)

if (( migration_count == 0 )); then
  echo "No versioned migrations found in $MIGRATION_DIR" >&2
  exit 1
fi

echo "Validated $migration_count sequential Flyway migrations (V1..V$((expected_version - 1)))."
