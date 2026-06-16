#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command npm
ensure_state_dirs
mkdir -p "${LOG_HOME}"

cd "${REPO_ROOT}/wimoorui"
if [[ ! -d node_modules ]]; then
  npm install
fi

nohup npm run dev -- --host 0.0.0.0 --port 8084 --open false >"$(service_log_file frontend)" 2>&1 &
echo $! > "$(service_pid_file frontend)"
wait_for_port 127.0.0.1 8084 120

echo "Frontend started on 8084."
