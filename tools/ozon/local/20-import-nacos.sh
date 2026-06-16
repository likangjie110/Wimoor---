#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command curl

NACOS_HTTP_BASE="http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs"
GROUP_NAME="DEFAULT_GROUP"
REQUIRED_IDS=(wimoor-common wimoor-commom-ext wimoor-admin wimoor-gateway wimoor-erp wimoor-ozon seataServer.properties)

config_file() {
  echo "${REPO_ROOT}/init-config/nacos/${GROUP_NAME}/$1"
}

config_type() {
  case "$1" in
    wimoor-common|wimoor-commom-ext|wimoor-admin|seataServer.properties) echo "properties" ;;
    *) echo "yaml" ;;
  esac
}

render_config() {
  local data_id="$1"
  local source_file
  source_file="$(config_file "${data_id}")"
  local rendered
  rendered="$(mktemp)"
  cp "${source_file}" "${rendered}"

  case "${data_id}" in
    wimoor-common)
      sed -i "s#^redis.host=.*#redis.host=${REDIS_HOST}#g" "${rendered}"
      sed -i "s#^redis.password=.*#redis.password=${REDIS_PASSWORD}#g" "${rendered}"
      sed -i "s#^mysql.host=.*#mysql.host=${MYSQL_HOST}#g" "${rendered}"
      sed -i "s#^mysql.port=.*#mysql.port=${MYSQL_PORT}#g" "${rendered}"
      sed -i "s#^mysql.username=.*#mysql.username=${MYSQL_USER}#g" "${rendered}"
      sed -i "s#^mysql.password=.*#mysql.password=${MYSQL_PASSWORD}#g" "${rendered}"
      sed -i "s#^spring.redis.host=.*#spring.redis.host= ${REDIS_HOST}#g" "${rendered}"
      sed -i "s#^spring.redis.password=.*#spring.redis.password= ${REDIS_PASSWORD}#g" "${rendered}"
      sed -i "s#^spring.datasource.username=.*#spring.datasource.username=${MYSQL_USER}#g" "${rendered}"
      sed -i "s#^spring.datasource.password=.*#spring.datasource.password=${MYSQL_PASSWORD}#g" "${rendered}"
      ;;
    wimoor-commom-ext)
      sed -i "s#^config.photo-server=.*#config.photo-server=127.0.0.1#g" "${rendered}"
      sed -i "s#^config.photo-server-url=.*#config.photo-server-url=http://127.0.0.1#g" "${rendered}"
      ;;
    seataServer.properties)
      sed -i "s#^store.db.url=.*#store.db.url=jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/seata?useUnicode=true\\&rewriteBatchedStatements=true\\&serverTimezone=Asia/Shanghai#g" "${rendered}"
      sed -i "s#^store.db.user=.*#store.db.user=${MYSQL_USER}#g" "${rendered}" || true
      sed -i "s#^store.db.password=.*#store.db.password=${MYSQL_PASSWORD}#g" "${rendered}" || true
      ;;
  esac

  echo "${rendered}"
}

auth_args=()
if [[ -n "${NACOS_USERNAME:-}" ]]; then
  auth_args+=(--data-urlencode "username=${NACOS_USERNAME}")
fi
if [[ -n "${NACOS_PASSWORD:-}" ]]; then
  auth_args+=(--data-urlencode "password=${NACOS_PASSWORD}")
fi

check_one() {
  local data_id="$1"
  local response
  response="$(curl -sfS -G "${NACOS_HTTP_BASE}" \
    --data-urlencode "search=accurate" \
    --data-urlencode "group=${GROUP_NAME}" \
    --data-urlencode "dataId=${data_id}" \
    "${auth_args[@]}" 2>/dev/null || true)"
  if [[ "${response}" == *"${data_id}"* ]]; then
    echo "[check] present: ${data_id}"
    return 0
  fi
  echo "[check] missing: ${data_id}"
  return 1
}

if [[ "${1:-}" == "--check" ]]; then
  status=0
  for data_id in "${REQUIRED_IDS[@]}"; do
    check_one "${data_id}" || status=1
  done
  exit "${status}"
fi

for data_id in "${REQUIRED_IDS[@]}"; do
  file_path="$(render_config "${data_id}")"
  [[ -f "${file_path}" ]] || { echo "Missing Nacos file: ${file_path}" >&2; exit 1; }
  file_type="$(config_type "${data_id}")"
  echo "[nacos] import ${data_id} (${file_type})"
  curl -sfS -X POST "${NACOS_HTTP_BASE}" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${GROUP_NAME}" \
    --data-urlencode "type=${file_type}" \
    --data-urlencode "content@${file_path}" \
    "${auth_args[@]}"
  echo
  rm -f "${file_path}"
done

echo "Nacos configs imported."
