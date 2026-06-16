#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
WORK_DIR="/tmp/wimoorui-build-${USER:-codex}"

rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"

rsync -a --delete --exclude node_modules "${PROJECT_DIR}/" "${WORK_DIR}/"
ln -sfn "${PROJECT_DIR}/node_modules" "${WORK_DIR}/node_modules"

cd "${WORK_DIR}"
npm run build

rm -rf "${PROJECT_DIR}/dist"
mkdir -p "${PROJECT_DIR}/dist"
rsync -a --delete "${WORK_DIR}/dist/" "${PROJECT_DIR}/dist/"

echo "Build completed via ${WORK_DIR}"
