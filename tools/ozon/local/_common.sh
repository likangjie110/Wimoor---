#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
DEFAULT_ENV_FILE="${SCRIPT_DIR}/.env.local"
STATE_DIR="${SCRIPT_DIR}/.state"
PID_DIR="${STATE_DIR}/pids"
AUTH_ID_STATE_FILE="${STATE_DIR}/auth_id"
SESSION_STATE_FILE="${STATE_DIR}/session_id"

load_local_env() {
  local env_file="${OZON_ENV_FILE:-${DEFAULT_ENV_FILE}}"
  if [[ ! -f "${env_file}" ]]; then
    echo "Missing local env file: ${env_file}" >&2
    echo "Copy ${SCRIPT_DIR}/00-env.example to ${env_file} and fill local values." >&2
    return 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "${env_file}"
  set +a
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    return 1
  fi
}

require_var() {
  local name="$1"
  if [[ -z "${!name:-}" ]]; then
    echo "Missing required env var: ${name}" >&2
    return 1
  fi
}

ensure_state_dirs() {
  mkdir -p "${STATE_DIR}" "${PID_DIR}"
}

mysql_cmd() {
  local args=(-h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}")
  if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
    args+=("-p${MYSQL_PASSWORD}")
  fi
  printf '%q ' mysql "${args[@]}"
}

mysql_socket_cmd() {
  local socket_user="${MYSQL_BOOTSTRAP_SOCKET_USER:-root}"
  printf '%q ' mysql -u "${socket_user}" --protocol=SOCKET
}

build_userinfo_header() {
  require_var LOCAL_USER_ID
  local company_id
  company_id="$(resolve_company_id)"
  export LOCAL_COMPANY_ID="${company_id}"
  python3 - <<'PY'
import json
import os
import urllib.parse

payload = {
    "id": os.environ["LOCAL_USER_ID"],
    "companyid": os.environ["LOCAL_COMPANY_ID"],
}
print(urllib.parse.quote(json.dumps(payload, ensure_ascii=False)))
PY
}

resolve_company_id() {
  local current="${LOCAL_COMPANY_ID:-}"
  if [[ "${current}" =~ ^[0-9]+$ ]]; then
    echo "${current}"
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e 'SELECT id FROM db_admin.t_shop ORDER BY id LIMIT 1'
}

wait_for_port() {
  local host="$1"
  local port="$2"
  local timeout_secs="${3:-120}"
  local start_ts
  start_ts="$(date +%s)"
  while true; do
    if (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; then
      return 0
    fi
    if (( "$(date +%s)" - start_ts >= timeout_secs )); then
      echo "Timed out waiting for ${host}:${port}" >&2
      return 1
    fi
    sleep 2
  done
}

service_pid_file() {
  echo "${PID_DIR}/$1.pid"
}

service_log_file() {
  mkdir -p "${LOG_HOME}"
  echo "${LOG_HOME}/$1.log"
}

save_auth_id_state() {
  local auth_id="$1"
  ensure_state_dirs
  printf '%s' "${auth_id}" > "${AUTH_ID_STATE_FILE}"
}

load_auth_id_state() {
  if [[ -f "${AUTH_ID_STATE_FILE}" ]]; then
    cat "${AUTH_ID_STATE_FILE}"
  fi
}

save_session_state() {
  local session_id="$1"
  ensure_state_dirs
  printf '%s' "${session_id}" > "${SESSION_STATE_FILE}"
}

load_session_state() {
  if [[ -f "${SESSION_STATE_FILE}" ]]; then
    cat "${SESSION_STATE_FILE}"
  fi
}

service_status() {
  local name="$1"
  local pid_file
  pid_file="$(service_pid_file "${name}")"
  if [[ -f "${pid_file}" ]]; then
    local pid
    pid="$(cat "${pid_file}")"
    if kill -0 "${pid}" >/dev/null 2>&1; then
      echo "${name}: running (${pid})"
      return 0
    fi
  fi
  echo "${name}: stopped"
  return 1
}

discover_auth_id_direct() {
  local base_url="$1"
  local x_userinfo
  x_userinfo="$(build_userinfo_header)"
  curl -sfS -H "X-USERINFO: ${x_userinfo}" "${base_url}/api/v1/auth/list" | python3 - <<'PY'
import json
import sys

payload = json.load(sys.stdin)
rows = payload.get("data") or []
print((rows[0] or {}).get("id", "") if rows else "")
PY
}

discover_auth_id_db() {
  if [[ -z "${OZON_CLIENT_ID:-}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT id FROM db_ozon.t_ozon_auth WHERE client_id='${OZON_CLIENT_ID}' ORDER BY create_time DESC LIMIT 1"
}

discover_latest_draft_id_db() {
  local auth_id="${1:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT id FROM db_ozon.t_ozon_listing_draft WHERE auth_id='${auth_id}' ORDER BY update_time DESC LIMIT 1"
}

discover_latest_publish_task_id_db() {
  local auth_id="${1:-}"
  local draft_id="${2:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  local where_clause="auth_id='${auth_id}'"
  if [[ -n "${draft_id}" ]]; then
    where_clause="${where_clause} AND draft_id='${draft_id}'"
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT id FROM db_ozon.t_ozon_listing_publish_task WHERE ${where_clause} ORDER BY update_time DESC LIMIT 1"
}

discover_latest_posting_id_db() {
  local auth_id="${1:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT id FROM db_ozon.t_ozon_posting WHERE auth_id='${auth_id}' ORDER BY update_time DESC LIMIT 1"
}

discover_latest_finance_task_id_db() {
  local auth_id="${1:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT id FROM db_ozon.t_ozon_report_task WHERE auth_id='${auth_id}' ORDER BY update_time DESC LIMIT 1"
}

discover_latest_chat_session_id_db() {
  local auth_id="${1:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT session_id FROM db_ozon.t_ozon_chat_session WHERE auth_id='${auth_id}' ORDER BY update_time DESC LIMIT 1"
}

discover_latest_ads_account_id_db() {
  local auth_id="${1:-}"
  if [[ -z "${auth_id}" ]]; then
    return 0
  fi
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e \
    "SELECT account_id FROM db_ozon.t_ozon_ads_account WHERE auth_id='${auth_id}' ORDER BY update_time DESC LIMIT 1"
}

login_admin_session() {
  local response
  response="$(curl -sS -X POST -H 'Content-Type: application/json' \
    -d '{"account":"admin@wimoor.com","password":"123456"}' \
    "http://127.0.0.1:8100/admin/api/v1/auth/login")"
  RESPONSE_JSON="${response}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RESPONSE_JSON"])
print(((payload.get("data") or {}).get("session")) or "")
PY
}
