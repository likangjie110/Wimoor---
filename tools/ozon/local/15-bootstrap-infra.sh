#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

ensure_command curl
ensure_command tar

INFRA_ROOT="/tmp/wimoor-ozon-local"
mkdir -p "${INFRA_ROOT}"

download_if_missing() {
  local url="$1"
  local archive="$2"
  if [[ -f "${archive}" ]]; then
    echo "[bootstrap] cached: ${archive}"
    return 0
  fi
  curl -L --fail --output "${archive}" "${url}"
}

extract_if_missing() {
  local archive="$1"
  local target_dir="$2"
  if [[ -d "${target_dir}" ]]; then
    echo "[bootstrap] extracted: ${target_dir}"
    return 0
  fi
  tar --no-same-owner -xf "${archive}" -C "${INFRA_ROOT}"
}

download_if_missing "https://github.com/alibaba/nacos/releases/download/2.3.0/nacos-server-2.3.0.tar.gz" "${INFRA_ROOT}/nacos-server-2.3.0.tar.gz"
extract_if_missing "${INFRA_ROOT}/nacos-server-2.3.0.tar.gz" "${INFRA_ROOT}/nacos"

download_if_missing "https://github.com/apache/incubator-seata/releases/download/v1.6.1/seata-server-1.6.1.tar.gz" "${INFRA_ROOT}/seata-server-1.6.1.tar.gz"
extract_if_missing "${INFRA_ROOT}/seata-server-1.6.1.tar.gz" "${INFRA_ROOT}/seata"

echo "Infra archives ready under ${INFRA_ROOT}."
