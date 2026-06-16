#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command curl

SESSION_ID="${SESSION_ID:-$(load_session_state)}"
SESSION_ID="${SESSION_ID:-$(login_admin_session)}"
if [[ -z "${SESSION_ID}" ]]; then
  echo "Unable to obtain jsessionid from admin login." >&2
  exit 1
fi
save_session_state "${SESSION_ID}"

curl -sfS -H "jsessionid: ${SESSION_ID}" "http://127.0.0.1:8099/ozon/api/v1/auth/list" >/dev/null
curl -sfS -H "jsessionid: ${SESSION_ID}" "http://127.0.0.1:8099/ozon/api/v1/meta/features" >/dev/null
curl -sfS -H "jsessionid: ${SESSION_ID}" "http://127.0.0.1:8099/ozon/api/v1/task/list?authId=$(load_auth_id_state)" >/dev/null || true

echo "Gateway smoke passed."
