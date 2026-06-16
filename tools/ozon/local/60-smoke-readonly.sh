#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command curl
ensure_command python3

OZON_DIRECT_BASE="http://127.0.0.1:8106/ozon"
X_USERINFO="$(build_userinfo_header)"
AUTH_ID="${AUTH_ID:-$(load_auth_id_state)}"
AUTH_ID="${AUTH_ID:-$(discover_auth_id_direct "${OZON_DIRECT_BASE}" || true)}"
AUTH_ID="${AUTH_ID:-$(discover_auth_id_db || true)}"
if [[ -z "${AUTH_ID}" ]]; then
  echo "AUTH_ID is required for read-only smoke." >&2
  exit 1
fi
save_auth_id_state "${AUTH_ID}"

DRAFT_ID="${DRAFT_ID:-$(discover_latest_draft_id_db "${AUTH_ID}" || true)}"
PUBLISH_TASK_ID="${PUBLISH_TASK_ID:-$(discover_latest_publish_task_id_db "${AUTH_ID}" "${DRAFT_ID:-}" || true)}"
POSTING_ID="${POSTING_ID:-$(discover_latest_posting_id_db "${AUTH_ID}" || true)}"
FINANCE_TASK_ID="${FINANCE_TASK_ID:-$(discover_latest_finance_task_id_db "${AUTH_ID}" || true)}"
CHAT_SESSION_ID="${CHAT_SESSION_ID:-$(discover_latest_chat_session_id_db "${AUTH_ID}" || true)}"
ADS_ACCOUNT_ID="${ADS_ACCOUNT_ID:-$(discover_latest_ads_account_id_db "${AUTH_ID}" || true)}"

call_get() {
  local path="$1"
  curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}${path}" >/dev/null
}

call_post() {
  local path="$1"
  local payload="$2"
  curl -sfS -X POST -H "X-USERINFO: ${X_USERINFO}" -H "Content-Type: application/json" \
    -d "${payload}" "${OZON_DIRECT_BASE}${path}" >/dev/null
}

call_get "/api/v1/meta/features"
call_get "/api/v1/auth/list"
call_get "/api/v1/auth/ping?authId=${AUTH_ID}"
call_get "/api/v1/seller/warehouse/list?authId=${AUTH_ID}"
call_get "/api/v1/seller/deliveryMethod/list?authId=${AUTH_ID}"
call_get "/api/v1/product/draft/list?authId=${AUTH_ID}"
call_get "/api/v1/product/category/tree?authId=${AUTH_ID}"
call_get "/api/v1/stock/snapshot/list?authId=${AUTH_ID}"
call_get "/api/v1/stock/task/list?authId=${AUTH_ID}"
call_get "/api/v1/price/snapshot/list?authId=${AUTH_ID}"
call_get "/api/v1/price/task/list?authId=${AUTH_ID}"
call_get "/api/v1/posting/list?authId=${AUTH_ID}"
call_get "/api/v1/task/list?authId=${AUTH_ID}"
call_get "/api/v1/error/list?authId=${AUTH_ID}"
call_get "/api/v1/finance/task/list?authId=${AUTH_ID}"
call_get "/api/v1/finance/transaction/list?authId=${AUTH_ID}"
call_get "/api/v1/chat/session/list?authId=${AUTH_ID}"
call_get "/api/v1/ads/account/list?authId=${AUTH_ID}"
call_get "/api/v1/ads/campaign/list?authId=${AUTH_ID}"
call_get "/api/v1/ads/report/list?authId=${AUTH_ID}"
call_get "/api/v1/ads/summary?authId=${AUTH_ID}"
call_get "/api/v1/ops/summary?authId=${AUTH_ID}"

if [[ -n "${DRAFT_ID}" ]]; then
  call_get "/api/v1/product/draft/detail?authId=${AUTH_ID}&draftId=${DRAFT_ID}"
  call_post "/api/v1/product/preview" "{\"authId\":\"${AUTH_ID}\",\"draftId\":\"${DRAFT_ID}\"}"
  call_get "/api/v1/product/publish/task/list?authId=${AUTH_ID}&draftId=${DRAFT_ID}"
fi

if [[ -n "${PUBLISH_TASK_ID}" ]]; then
  call_get "/api/v1/product/publish/task/detail?authId=${AUTH_ID}&taskId=${PUBLISH_TASK_ID}"
fi

if [[ -n "${POSTING_ID}" ]]; then
  call_get "/api/v1/posting/detail?authId=${AUTH_ID}&postingId=${POSTING_ID}"
  call_get "/api/v1/shipment/list?authId=${AUTH_ID}&postingId=${POSTING_ID}"
  call_get "/api/v1/posting/aftersale/detail?authId=${AUTH_ID}&postingId=${POSTING_ID}"
  call_get "/api/v1/ops/api-log/list?authId=${AUTH_ID}&objectId=${POSTING_ID}"
  call_get "/api/v1/ops/operation-audit/list?authId=${AUTH_ID}&objectId=${POSTING_ID}"
fi

if [[ -n "${FINANCE_TASK_ID}" ]]; then
  call_get "/api/v1/finance/task/raw?authId=${AUTH_ID}&taskId=${FINANCE_TASK_ID}"
fi

if [[ -n "${CHAT_SESSION_ID}" ]]; then
  call_get "/api/v1/chat/message/list?authId=${AUTH_ID}&sessionId=${CHAT_SESSION_ID}"
  call_get "/api/v1/chat/reply/audit/list?authId=${AUTH_ID}&sessionId=${CHAT_SESSION_ID}"
fi

if [[ -n "${ADS_ACCOUNT_ID}" ]]; then
  call_get "/api/v1/ads/campaign/list?authId=${AUTH_ID}&accountId=${ADS_ACCOUNT_ID}"
  call_get "/api/v1/ads/report/list?authId=${AUTH_ID}&accountId=${ADS_ACCOUNT_ID}"
  call_get "/api/v1/ads/summary?authId=${AUTH_ID}&accountId=${ADS_ACCOUNT_ID}"
fi

echo "Read-only smoke passed."
