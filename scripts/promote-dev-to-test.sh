#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
START_BRANCH="$(git -C "$REPO_ROOT" branch --show-current)"

run_local_build_checks() {
  echo "== build backend (mvn -DskipTests package) =="
  mvn -f "$REPO_ROOT/app/pom.xml" -DskipTests package

  echo "== build frontend (npm run build) =="
  npm --prefix "$REPO_ROOT/web" run build
}

cleanup() {
  if [[ -n "$START_BRANCH" ]] && [[ "$(git -C "$REPO_ROOT" branch --show-current)" != "$START_BRANCH" ]]; then
    git -C "$REPO_ROOT" checkout "$START_BRANCH" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

if [[ "$START_BRANCH" != "dev" ]]; then
  echo "Current branch is '$START_BRANCH'. Switch to 'dev' before promotion." >&2
  exit 1
fi

if [[ -n "$(git -C "$REPO_ROOT" status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit or stash changes before promotion." >&2
  exit 1
fi

echo "== fetch origin =="
git -C "$REPO_ROOT" fetch origin

echo "== update local dev =="
git -C "$REPO_ROOT" pull --rebase origin dev

echo "== validate local builds =="
run_local_build_checks

echo "== push dev =="
git -C "$REPO_ROOT" push origin dev

echo "== switch to test =="
git -C "$REPO_ROOT" checkout test

echo "== update local test =="
git -C "$REPO_ROOT" pull --rebase origin test

echo "== merge dev into test =="
git -C "$REPO_ROOT" merge --no-edit dev

echo "== push test =="
git -C "$REPO_ROOT" push origin test

echo "== return to dev =="
git -C "$REPO_ROOT" checkout dev

echo "Promotion dev -> test completed."