#!/usr/bin/env bash
set -euo pipefail

SERVER_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SERVER_SCRIPT_DIR}/../local/_common.sh"

load_local_env
ensure_command curl
ensure_command python3

FRONTEND_PORT="${FRONTEND_PORT:-8084}"
BASE_URL="${BASE_URL:-http://127.0.0.1:${FRONTEND_PORT}}"

login_response="$(curl -sfS -X POST -H 'Content-Type: application/json' \
  -d '{"account":"admin@wimoor.com","password":"123456"}' \
  "http://127.0.0.1/admin/api/v1/auth/login")"

session_id="$(
  RESPONSE_JSON="${login_response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE_JSON"])
print(((payload.get("data") or {}).get("session")) or "")
PY
)"

[[ -n "${session_id}" ]] || { echo "Unable to obtain jsessionid from existing admin service." >&2; exit 1; }

curl -sfS "${BASE_URL}/ozon/api/v1/meta/features" >/dev/null
curl -sfS -H "jsessionid: ${session_id}" "${BASE_URL}/ozon/api/v1/auth/list" >/dev/null

echo "Ozon smoke passed via ${BASE_URL}."
