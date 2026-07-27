#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

required_examples=(
  ".env.example"
  "web/.env.example"
  "ops/env/common.env.example"
  "ops/env/app.env.example"
  "ops/env/mailer.env.example"
)

for path in "${required_examples[@]}"; do
  [[ -f "$REPO_ROOT/$path" ]] || { echo "Missing secret contract example: $path" >&2; exit 1; }
done

while IFS= read -r path; do
  case "$path" in
    *.env.example|.env.example)
      ;;
    *)
      echo "Tracked environment file is forbidden: $path" >&2
      exit 1
      ;;
  esac
done < <(git -C "$REPO_ROOT" ls-files '*.env' '*.env.*')

sensitive_paths="$(
  git -C "$REPO_ROOT" ls-files \
    | grep -E '(^|/)(id_rsa|id_ed25519)$|\.(pem|key|p12|pfx|jks|keystore)$' \
    || true
)"
if [[ -n "$sensitive_paths" ]]; then
  echo "Tracked key or keystore files are forbidden:" >&2
  echo "$sensitive_paths" >&2
  exit 1
fi

if git -C "$REPO_ROOT" grep -IEn \
  'BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|github_pat_[A-Za-z0-9_]{20,}|ghp_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}' \
  -- ':!scripts/validate-secret-layout.sh'; then
  echo "Possible committed secret detected." >&2
  exit 1
fi

required_local_keys=(DB_PASSWORD JWT_SECRET MAILER_TRANSPORT_TYPE MAILER_SMTP_PASSWORD)
for key in "${required_local_keys[@]}"; do
  grep -Eq "^${key}=" "$REPO_ROOT/.env.example" || {
    echo ".env.example is missing $key." >&2
    exit 1
  }
done

deploy_workflow="$REPO_ROOT/.github/workflows/deploy-test.yml"
for contract_entry in 'environment: test' 'secrets.VPS_HOST' 'secrets.VPS_USER' 'secrets.VPS_SSH_KEY' 'secrets.VPS_HOST_FINGERPRINT' 'vars.PUBLIC_BASE_URL'; do
  grep -Fq "$contract_entry" "$deploy_workflow" || {
    echo "Deploy workflow is missing secret contract entry: $contract_entry" >&2
    exit 1
  }
done
if grep -Eq 'TEST_VPS_|^[[:space:]]+password:' "$deploy_workflow"; then
  echo "Deploy workflow must use environment-scoped SSH key authentication." >&2
  exit 1
fi

echo "Secret layout is valid; no tracked runtime env or private key files found."
