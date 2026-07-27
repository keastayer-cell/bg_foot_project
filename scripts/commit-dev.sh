#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CURRENT_BRANCH="$(git -C "$REPO_ROOT" branch --show-current)"

if [[ "$CURRENT_BRANCH" != "dev" ]]; then
  echo "Current branch is '$CURRENT_BRANCH'. Switch to 'dev' before committing." >&2
  exit 1
fi

COMMIT_MESSAGE="${1:-}"
if [[ -z "$COMMIT_MESSAGE" ]]; then
  echo "Commit message is required." >&2
  exit 1
fi

if [[ -z "$(git -C "$REPO_ROOT" status --porcelain)" ]]; then
  echo "Working tree is clean. Nothing to commit." >&2
  exit 1
fi

echo "== stage changes =="
git -C "$REPO_ROOT" add -A

echo "== commit to dev =="
git -C "$REPO_ROOT" commit -m "$COMMIT_MESSAGE"

echo "Commit created on dev."