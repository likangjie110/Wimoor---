#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command mvn
ensure_state_dirs
mkdir -p "${LOG_HOME}"
mkdir -p "${JM_LOG_PATH:-/tmp/wimoor-logs/nacos}" "${JM_SNAPSHOT_PATH:-/tmp/wimoor-nacos-snapshot}"

start_service() {
  local name="$1"
  local pom_path="$2"
  local port="$3"
  if (echo >"/dev/tcp/127.0.0.1/${port}") >/dev/null 2>&1; then
    echo "${name} already running on ${port}"
    return 0
  fi
  local service_log_home="${LOG_HOME}/${name}"
  local log_file
  local jvm_args
  case "${name}" in
    gateway) jvm_args="-Xms128m -Xmx256m" ;;
    erp) jvm_args="-Xms256m -Xmx768m" ;;
    *) jvm_args="-Xms128m -Xmx512m" ;;
  esac
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
    mvn -Dmaven.repo.local=/tmp/m2 -f "${pom_path}" -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="${jvm_args}" \
    >"${log_file}" 2>&1 < /dev/null &
  echo $! > "$(service_pid_file "${name}")"
  wait_for_port 127.0.0.1 "${port}" 300
  echo "${name} started on ${port}"
}

start_service admin "${REPO_ROOT}/wimoor-admin/admin-boot/pom.xml" 8100
start_service gateway "${REPO_ROOT}/wimoor-gateway/pom.xml" 8099
start_service erp "${REPO_ROOT}/wimoor-erp/erp-boot/pom.xml" 8101
start_service ozon "${REPO_ROOT}/wimoor-ozon/ozon-boot/pom.xml" 8106

echo "Backend stack started."
