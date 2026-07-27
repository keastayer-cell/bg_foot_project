#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_REF="${1:-}"
MIGRATION_PATH="app/src/main/resources/db/migration"

if [[ -z "$BASE_REF" ]]; then
  echo "Usage: $0 <base-git-ref>" >&2
  exit 2
fi

if ! git -C "$REPO_ROOT" rev-parse --verify "$BASE_REF^{commit}" >/dev/null 2>&1; then
  echo "Unknown base git ref: $BASE_REF" >&2
  exit 1
fi

base_max=0
while IFS= read -r path; do
  filename="${path##*/}"
  if [[ "$filename" =~ ^V([0-9]+)__ ]]; then
    version=$((10#${BASH_REMATCH[1]}))
    (( version > base_max )) && base_max=$version
  fi
done < <(git -C "$REPO_ROOT" ls-tree -r --name-only "$BASE_REF" -- "$MIGRATION_PATH")

while IFS=$'\t' read -r status old_path new_path; do
  [[ -z "$status" ]] && continue
  path="${new_path:-$old_path}"
  filename="${path##*/}"

  case "$status" in
    A*)
      if [[ ! "$filename" =~ ^V([0-9]+)__ ]]; then
        echo "Invalid new migration name: $path" >&2
        exit 1
      fi
      version=$((10#${BASH_REMATCH[1]}))
      if (( version <= base_max )); then
        echo "New migration $filename must be newer than V$base_max." >&2
        exit 1
      fi
      ;;
    *)
      echo "Published migration must not be modified, deleted or renamed: $path ($status)" >&2
      exit 1
      ;;
  esac
done < <(git -C "$REPO_ROOT" diff --name-status "$BASE_REF"...HEAD -- "$MIGRATION_PATH")

echo "Migration history is append-only relative to $BASE_REF (base max V$base_max)."
