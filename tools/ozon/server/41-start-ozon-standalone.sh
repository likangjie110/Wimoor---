#!/usr/bin/env bash
set -euo pipefail

SERVER_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SERVER_SCRIPT_DIR}/../local/_common.sh"

load_local_env
ensure_state_dirs

ARTIFACT_ROOT="${ARTIFACT_ROOT:-${REPO_ROOT}/.deploy/ozon}"
CONFIG_DIR="${CONFIG_DIR:-${ARTIFACT_ROOT}/config}"
JAR_PATH="${JAR_PATH:-${ARTIFACT_ROOT}/ozon-boot.jar}"
OZON_PORT="${OZON_PORT:-8106}"
OZON_JAVA_OPTS="${OZON_JAVA_OPTS:--Xms256m -Xmx384m}"
LOG_HOME="${LOG_HOME:-/tmp/wimoor-logs}"
JAVA_BIN="${JAVA_BIN:-${ARTIFACT_ROOT}/jre/bin/java}"

if [[ ! -x "${JAVA_BIN}" ]]; then
  JAVA_BIN="$(command -v java || true)"
fi
[[ -n "${JAVA_BIN}" ]] || { echo "java not found. Set JAVA_BIN or bundle a JRE under ${ARTIFACT_ROOT}/jre." >&2; exit 1; }

[[ -f "${JAR_PATH}" ]] || { echo "Missing jar: ${JAR_PATH}" >&2; exit 1; }

if [[ ! -f "${CONFIG_DIR}/application.yml" ]]; then
  bash "${SERVER_SCRIPT_DIR}/30-render-ozon-standalone-config.sh"
fi

if (echo >"/dev/tcp/127.0.0.1/${OZON_PORT}") >/dev/null 2>&1; then
  echo "ozon already running on ${OZON_PORT}"
  exit 0
fi

mkdir -p "${LOG_HOME}/ozon"

nohup env \
  LOG_HOME="${LOG_HOME}/ozon" \
  "JM.LOG.PATH=${JM_LOG_PATH:-/tmp/wimoor-logs/nacos}" \
  "JM.SNAPSHOT.PATH=${JM_SNAPSHOT_PATH:-/tmp/wimoor-nacos-snapshot}" \
  "${JAVA_BIN}" ${OZON_JAVA_OPTS} -jar "${JAR_PATH}" \
  --spring.config.additional-location="file:${CONFIG_DIR}/" \
  >"$(service_log_file ozon)" 2>&1 < /dev/null &

echo $! > "$(service_pid_file ozon)"
wait_for_port 127.0.0.1 "${OZON_PORT}" 300

echo "Ozon standalone started on ${OZON_PORT}."
