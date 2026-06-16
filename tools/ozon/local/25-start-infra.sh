#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_state_dirs

INFRA_ROOT="/tmp/wimoor-ozon-local"
NACOS_DIR="${INFRA_ROOT}/nacos"
SEATA_DIR="${INFRA_ROOT}/seata"
JAVA_BIN="${JAVA_BIN:-$(command -v java || true)}"
NACOS_JVM_OPTS="${NACOS_JVM_OPTS:--Xms256m -Xmx256m -Xmn128m}"
JAVA_EXT_DIRS="${JAVA_EXT_DIRS:-}"

[[ -d "${NACOS_DIR}" ]] || { echo "Missing ${NACOS_DIR}. Run 15-bootstrap-infra.sh first." >&2; exit 1; }
[[ -d "${SEATA_DIR}" ]] || { echo "Missing ${SEATA_DIR}. Run 15-bootstrap-infra.sh first." >&2; exit 1; }
[[ -n "${JAVA_BIN}" ]] || { echo "java not found. Install JDK 8+ or set JAVA_BIN." >&2; exit 1; }

mkdir -p "${JM_LOG_PATH:-/tmp/wimoor-logs/nacos}" "${JM_SNAPSHOT_PATH:-/tmp/wimoor-nacos-snapshot}" "${LOG_HOME}/infra"

start_nacos() {
  local log_file
  log_file="$(service_log_file nacos)"
  mkdir -p "${NACOS_DIR}/logs"
  local java_ext_arg=()
  if [[ -n "${JAVA_EXT_DIRS}" ]]; then
    java_ext_arg+=("-Djava.ext.dirs=${JAVA_EXT_DIRS}")
  fi
  nohup env \
    "${JAVA_BIN}" \
    "${java_ext_arg[@]}" \
    ${NACOS_JVM_OPTS} -Dnacos.standalone=true \
    -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70 \
    -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 \
    -Xloggc:${NACOS_DIR}/logs/nacos_gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps \
    -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=100M \
    -Dloader.path=${NACOS_DIR}/plugins,${NACOS_DIR}/plugins/health,${NACOS_DIR}/plugins/cmdb,${NACOS_DIR}/plugins/selector \
    -Dnacos.home=${NACOS_DIR} \
    -jar ${NACOS_DIR}/target/nacos-server.jar \
    --spring.config.additional-location=file:${NACOS_DIR}/conf/ \
    --logging.config=${NACOS_DIR}/conf/nacos-logback.xml \
    --server.max-http-header-size=524288 \
    >"${log_file}" 2>&1 < /dev/null &
  echo $! > "$(service_pid_file nacos)"
  wait_for_port 127.0.0.1 8848 180
}

start_seata() {
  local seata_conf="${SEATA_DIR}/conf/application.yml"
  cp "${REPO_ROOT}/init-config/seata/seata-application.yml" "${seata_conf}"
  sed -i "s#\${user.home}/logs/seata#${LOG_HOME}/seata#g" "${seata_conf}"
  sed -i "s#server-addr: 127.0.0.1:8848#server-addr: ${NACOS_SERVER_ADDR}#g" "${seata_conf}"
  sed -i "s#namespace: \$#namespace: ${NACOS_NAMESPACE}#g" "${seata_conf}"
  if [[ -n "${NACOS_USERNAME:-}" ]]; then
    sed -i "s#username: nacos#username: ${NACOS_USERNAME}#g" "${seata_conf}"
  fi
  if [[ -n "${NACOS_PASSWORD:-}" ]]; then
    sed -i "s#password: nacos#password: ${NACOS_PASSWORD}#g" "${seata_conf}"
  fi
  mkdir -p "${LOG_HOME}/seata"
  local log_file
  log_file="$(service_log_file seata)"
  nohup env \
    SEATA_IP=127.0.0.1 \
    bash -lc "cd '${SEATA_DIR}' && exec bash '${SEATA_DIR}/bin/seata-server.sh'" \
    >"${log_file}" 2>&1 < /dev/null &
  echo $! > "$(service_pid_file seata)"
  wait_for_port 127.0.0.1 8091 180
}

start_nacos
bash "${SCRIPT_DIR}/20-import-nacos.sh"
start_seata

echo "Infra stack started."
