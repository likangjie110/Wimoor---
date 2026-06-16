#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/../local/_common.sh"

load_local_env
ensure_state_dirs
ensure_command java

ARTIFACT_ROOT="${ARTIFACT_ROOT:-${REPO_ROOT}/.deploy/ozon}"
LOG_HOME="${LOG_HOME:-/tmp/wimoor-logs}"
mkdir -p "${LOG_HOME}"

ADMIN_PORT="${ADMIN_PORT:-8100}"
GATEWAY_PORT="${GATEWAY_PORT:-8099}"
ERP_PORT="${ERP_PORT:-8101}"
OZON_PORT="${OZON_PORT:-8106}"

ADMIN_JAVA_OPTS="${ADMIN_JAVA_OPTS:--Xms128m -Xmx256m}"
GATEWAY_JAVA_OPTS="${GATEWAY_JAVA_OPTS:--Xms96m -Xmx192m}"
ERP_JAVA_OPTS="${ERP_JAVA_OPTS:--Xms256m -Xmx384m}"
OZON_JAVA_OPTS="${OZON_JAVA_OPTS:--Xms256m -Xmx384m}"

start_jar() {
  local name="$1"
  local jar_path="$2"
  local port="$3"
  local jvm_args="$4"

  [[ -f "${jar_path}" ]] || { echo "Missing jar: ${jar_path}" >&2; exit 1; }

  if (echo >"/dev/tcp/127.0.0.1/${port}") >/dev/null 2>&1; then
    echo "${name} already running on ${port}"
    return 0
  fi

  local service_log_home="${LOG_HOME}/${name}"
  local log_file
  log_file="$(service_log_file "${name}")"
  mkdir -p "${service_log_home}"

  nohup env \
    NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR}" \
    NACOS_NAMESPACE="${NACOS_NAMESPACE}" \
    NACOS_IP="${NACOS_IP}" \
    OZON_SECURITY_AES_KEY="${OZON_SECURITY_AES_KEY:-}" \
    CHANNEL_CREDENTIAL_AES_KEY="${OZON_SECURITY_AES_KEY:-}" \
    LOG_HOME="${service_log_home}" \
    "JM.LOG.PATH=${JM_LOG_PATH:-/tmp/wimoor-logs/nacos}" \
    "JM.SNAPSHOT.PATH=${JM_SNAPSHOT_PATH:-/tmp/wimoor-nacos-snapshot}" \
    java ${jvm_args} -jar "${jar_path}" --spring.profiles.active=dev \
    >"${log_file}" 2>&1 < /dev/null &

  echo $! > "$(service_pid_file "${name}")"
  wait_for_port 127.0.0.1 "${port}" 300
  echo "${name} started on ${port}"
}

start_jar admin "${ARTIFACT_ROOT}/admin-boot.jar" "${ADMIN_PORT}" "${ADMIN_JAVA_OPTS}"
start_jar gateway "${ARTIFACT_ROOT}/wimoor-gateway.jar" "${GATEWAY_PORT}" "${GATEWAY_JAVA_OPTS}"
start_jar erp "${ARTIFACT_ROOT}/erp-boot.jar" "${ERP_PORT}" "${ERP_JAVA_OPTS}"
start_jar ozon "${ARTIFACT_ROOT}/ozon-boot.jar" "${OZON_PORT}" "${OZON_JAVA_OPTS}"

echo "Backend jar stack started."
