#!/usr/bin/env bash
set -euo pipefail

SERVER_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SERVER_SCRIPT_DIR}/../local/_common.sh"

load_local_env
ensure_state_dirs
ensure_command node

ARTIFACT_ROOT="${ARTIFACT_ROOT:-${REPO_ROOT}/.deploy/ozon}"
DIST_DIR="${DIST_DIR:-${ARTIFACT_ROOT}/frontend/dist}"
FRONTEND_PORT="${FRONTEND_PORT:-8084}"
GATEWAY_ORIGIN="${GATEWAY_ORIGIN:-http://127.0.0.1:8099}"
OZON_ORIGIN="${OZON_ORIGIN:-http://127.0.0.1:8106}"
STATIC_ORIGIN="${STATIC_ORIGIN:-http://127.0.0.1}"
LOG_HOME="${LOG_HOME:-/tmp/wimoor-logs}"

if (echo >"/dev/tcp/127.0.0.1/${FRONTEND_PORT}") >/dev/null 2>&1; then
  echo "frontend already running on ${FRONTEND_PORT}"
  exit 0
fi

mkdir -p "${LOG_HOME}"

nohup env \
  DIST_DIR="${DIST_DIR}" \
  PORT="${FRONTEND_PORT}" \
  GATEWAY_ORIGIN="${GATEWAY_ORIGIN}" \
  OZON_ORIGIN="${OZON_ORIGIN}" \
  STATIC_ORIGIN="${STATIC_ORIGIN}" \
  node "${SERVER_SCRIPT_DIR}/frontend-server.mjs" \
  >"$(service_log_file frontend)" 2>&1 < /dev/null &

echo $! > "$(service_pid_file frontend)"
wait_for_port 127.0.0.1 "${FRONTEND_PORT}" 120

echo "Frontend started on ${FRONTEND_PORT}."
