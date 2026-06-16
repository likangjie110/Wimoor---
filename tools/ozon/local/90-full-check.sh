#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command mvn
ensure_command node
ensure_command bash
mkdir -p "${JM_LOG_PATH:-/tmp/wimoor-logs/nacos}" "${JM_SNAPSHOT_PATH:-/tmp/wimoor-nacos-snapshot}"

REPORT_DIR="${REPO_ROOT}/docs/superpowers/verification"
mkdir -p "${REPORT_DIR}"
REPORT_FILE="${REPORT_DIR}/ozon-local-report-$(date +%Y%m%d-%H%M).md"
HANDOFF_SCRIPT="${SCRIPT_DIR}/95-release-handoff.sh"
SUMMARY_SCRIPT="${SCRIPT_DIR}/96-verify-summary.sh"
RELEASE_NOTE_SCRIPT="${SCRIPT_DIR}/97-release-note.sh"
INDEX_SCRIPT="${SCRIPT_DIR}/98-verify-index.sh"
PLAYWRIGHT_CACHE_DIR="${HOME}/.cache/ms-playwright"

run_and_log() {
  local label="$1"
  shift
  echo "## ${label}" >> "${REPORT_FILE}"
  echo '```bash' >> "${REPORT_FILE}"
  printf '%q ' "$@" >> "${REPORT_FILE}"
  echo >> "${REPORT_FILE}"
  echo '```' >> "${REPORT_FILE}"
  "$@"
  echo >> "${REPORT_FILE}"
}

ensure_playwright_chromium() {
  if [[ "${SKIP_PLAYWRIGHT_INSTALL:-0}" == "1" ]]; then
    echo "Skip Playwright install because SKIP_PLAYWRIGHT_INSTALL=1"
    return 0
  fi
  if compgen -G "${PLAYWRIGHT_CACHE_DIR}/chromium-*" >/dev/null 2>&1; then
    echo "Playwright Chromium already present in ${PLAYWRIGHT_CACHE_DIR}"
    return 0
  fi
  bash -lc "cd '${REPO_ROOT}/wimoorui' && npx playwright install chromium"
}

echo "# Ozon Local Report" > "${REPORT_FILE}"
echo >> "${REPORT_FILE}"
echo "Generated at $(date '+%Y-%m-%d %H:%M:%S %z')" >> "${REPORT_FILE}"
echo >> "${REPORT_FILE}"
echo "## Git Status" >> "${REPORT_FILE}"
echo '```text' >> "${REPORT_FILE}"
git -C "${REPO_ROOT}" status --short >> "${REPORT_FILE}"
echo '```' >> "${REPORT_FILE}"
echo >> "${REPORT_FILE}"

run_and_log "Ozon Backend Tests" mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests,OzonProductMetadataServiceTests,OzonProductPreviewServiceTests,OzonProductPublishServiceTests,OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test
run_and_log "Ozon Entry Check" node "${REPO_ROOT}/wimoorui/scripts/check_ozon_entry.mjs"
run_and_log "Ozon Product Entry Check" node "${REPO_ROOT}/wimoorui/scripts/check_ozon_product_publish_entry.mjs"
run_and_log "Ozon Route Smoke Check" node "${REPO_ROOT}/wimoorui/scripts/check_ozon_route_smoke.mjs"
run_and_log "Ensure Playwright Chromium" ensure_playwright_chromium
run_and_log "Ozon Frontend E2E" bash -lc "cd '${REPO_ROOT}/wimoorui' && npm run test:e2e:ozon"
run_and_log "Frontend Build" bash -lc "cd '${REPO_ROOT}/wimoorui' && timeout 900s ./scripts/build_in_linux_fs.sh"
run_and_log "Frontend Audit" bash -lc "cd '${REPO_ROOT}/wimoorui' && npm audit --omit=dev"
run_and_log "Amazon Package" mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon/amazon-boot -am -DskipTests clean package
run_and_log "Amazon Adv Package" mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon-adv/amazon-adv-boot -am -DskipTests clean package
run_and_log "Bootstrap Infra" bash "${SCRIPT_DIR}/15-bootstrap-infra.sh"
run_and_log "Start Infra" bash "${SCRIPT_DIR}/25-start-infra.sh"
run_and_log "Install Backend Reactor Artifacts" bash "${SCRIPT_DIR}/30-install-reactor-artifacts.sh"
run_and_log "Start Backend" bash "${SCRIPT_DIR}/40-start-backend.sh"
run_and_log "Start Frontend" bash "${SCRIPT_DIR}/50-start-frontend.sh"
if [[ -z "${AUTH_ID:-}" && -n "${OZON_CLIENT_ID:-}" && -n "${OZON_API_KEY:-}" ]]; then
  run_and_log "Bootstrap Local Auth" bash -lc \
    "source '${SCRIPT_DIR}/_common.sh'; \
     load_local_env; \
     X_USERINFO=\"\$(build_userinfo_header)\"; \
     curl -sfS -X POST -H \"X-USERINFO: \${X_USERINFO}\" -H 'Content-Type: application/json' \
       -d '{\"name\":\"local-ozon\",\"clientId\":\"'\"'\"'\${OZON_CLIENT_ID}'\"'\"'\",\"apiKey\":\"'\"'\"'\${OZON_API_KEY}'\"'\"'\"}' \
       'http://127.0.0.1:8106/ozon/api/v1/auth/bind' >/dev/null || true"
fi
run_and_log "Gateway Smoke" bash "${SCRIPT_DIR}/65-smoke-gateway.sh"
run_and_log "Readonly Smoke" bash "${SCRIPT_DIR}/60-smoke-readonly.sh"
run_and_log "Write Smoke" bash "${SCRIPT_DIR}/70-smoke-write.sh"

HANDOFF_FILE="$("${HANDOFF_SCRIPT}" "${REPORT_FILE}")"
SUMMARY_FILE="$("${SUMMARY_SCRIPT}" "${REPORT_FILE}" "${HANDOFF_FILE}")"
RELEASE_NOTE_FILE="$("${RELEASE_NOTE_SCRIPT}" "${HANDOFF_FILE}" "${SUMMARY_FILE}")"
INDEX_FILE="$("${INDEX_SCRIPT}")"

echo "Report written to ${REPORT_FILE}"
echo "Handoff written to ${HANDOFF_FILE}"
echo "Summary written to ${SUMMARY_FILE}"
echo "Release note written to ${RELEASE_NOTE_FILE}"
echo "Index written to ${INDEX_FILE}"
