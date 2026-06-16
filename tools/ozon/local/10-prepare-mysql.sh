#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command mysql

declare -A STRUCTURE_DIRS=(
  [seata]="${REPO_ROOT}/init-config/mysql/数据库结构/seata"
  [db_quartz]="${REPO_ROOT}/init-config/mysql/数据库结构/db_quartz"
  [db_admin]="${REPO_ROOT}/init-config/mysql/数据库结构/db_admin"
  [db_erp]="${REPO_ROOT}/init-config/mysql/数据库结构/db_erp"
  [db_ozon]="${REPO_ROOT}/init-config/mysql/数据库结构/db_ozon"
  [db_amazon]="${REPO_ROOT}/init-config/mysql/数据库结构/db_amazon"
  [db_amazon_adv]="${REPO_ROOT}/init-config/mysql/数据库结构/db_amazon_adv"
)

declare -A DATA_DIRS=(
  [db_admin]="${REPO_ROOT}/init-config/mysql/数据/db_admin"
  [db_erp]="${REPO_ROOT}/init-config/mysql/数据/db_erp"
  [db_amazon]="${REPO_ROOT}/init-config/mysql/数据/db_amazon"
  [db_amazon_adv]="${REPO_ROOT}/init-config/mysql/数据/db_amazon_adv"
)

REQUIRED_DATABASES=(seata db_quartz db_admin db_erp db_ozon)
OPTIONAL_DATABASES=(db_amazon db_amazon_adv)

mysql_exec() {
  local sql="$1"
  mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" ${MYSQL_PASSWORD:+-p${MYSQL_PASSWORD}} -N -e "${sql}"
}

mysql_exec_socket() {
  local sql="$1"
  mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET -N -e "${sql}"
}

db_exists() {
  local db="$1"
  [[ -n "$(mysql_exec "SHOW DATABASES LIKE '${db}';")" ]]
}

create_db() {
  local db="$1"
  mysql_exec_socket "CREATE DATABASE IF NOT EXISTS \`${db}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;"
}

ensure_app_user() {
  local user="${MYSQL_USER}"
  local password="${MYSQL_PASSWORD:-}"
  if [[ -z "${user}" || "${user}" == "root" ]]; then
    return 0
  fi
  mysql_exec_socket "CREATE USER IF NOT EXISTS '${user}'@'localhost' IDENTIFIED BY '${password}';"
  mysql_exec_socket "CREATE USER IF NOT EXISTS '${user}'@'127.0.0.1' IDENTIFIED BY '${password}';"
  for db in "${REQUIRED_DATABASES[@]}" "${OPTIONAL_DATABASES[@]}"; do
    mysql_exec_socket "GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'localhost';"
    mysql_exec_socket "GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'127.0.0.1';"
  done
  mysql_exec_socket "FLUSH PRIVILEGES;"
}

run_dir() {
  local db="$1"
  local dir="$2"
  mapfile -t files < <(find "${dir}" -maxdepth 1 -type f -name '*.sql' | sort)
  for sql_file in "${files[@]}"; do
    echo "[mysql] ${db} <= ${sql_file}"
    mysql -u "${MYSQL_BOOTSTRAP_SOCKET_USER:-root}" --protocol=SOCKET --force "${db}" < "${sql_file}"
  done
}

check_only() {
  local failed=0
  for db in "${REQUIRED_DATABASES[@]}" "${OPTIONAL_DATABASES[@]}"; do
    if [[ -d "${STRUCTURE_DIRS[${db}]}" ]]; then
      echo "[check] structure ok: ${db}"
    else
      echo "[check] missing structure dir: ${db}" >&2
      failed=1
    fi
    if [[ -n "${DATA_DIRS[${db}]:-}" && -d "${DATA_DIRS[${db}]}" ]]; then
      echo "[check] data ok: ${db}"
    fi
    if db_exists "${db}"; then
      echo "[check] database exists: ${db}"
    else
      echo "[check] database missing: ${db}"
    fi
  done
  return "${failed}"
}

if [[ "${1:-}" == "--check" ]]; then
  check_only
  exit 0
fi

for db in "${REQUIRED_DATABASES[@]}" "${OPTIONAL_DATABASES[@]}"; do
  [[ -d "${STRUCTURE_DIRS[${db}]}" ]] || continue
  create_db "${db}"
  run_dir "${db}" "${STRUCTURE_DIRS[${db}]}"
  if [[ -n "${DATA_DIRS[${db}]:-}" && -d "${DATA_DIRS[${db}]}" ]]; then
    run_dir "${db}" "${DATA_DIRS[${db}]}"
  fi
done

ensure_app_user

echo "MySQL schemas and seeds imported."
