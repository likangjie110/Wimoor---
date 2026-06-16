#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command curl
ensure_command python3

require_var OZON_CLIENT_ID
require_var OZON_API_KEY
ERP_TEST_SKU="${ERP_TEST_SKU:-ERP-SKU-1}"

OZON_DIRECT_BASE="http://127.0.0.1:8106/ozon"
X_USERINFO="$(build_userinfo_header)"
NACOS_HTTP_BASE="http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs"

post_json() {
  local path="$1"
  local payload="$2"
  curl -sfS -X POST -H "X-USERINFO: ${X_USERINFO}" -H "Content-Type: application/json" \
    -d "${payload}" "${OZON_DIRECT_BASE}${path}"
}

assert_success_json() {
  local raw="$1"
  RAW_JSON="${raw}" python3 - <<'PY'
import json
import os
payload = json.loads(os.environ["RAW_JSON"])
if payload.get("code") != 200:
    raise SystemExit(f"Unexpected response: {payload}")
PY
}

enable_local_write_flags() {
  local tmp_file
  tmp_file="$(mktemp)"
  cp "${REPO_ROOT}/init-config/nacos/DEFAULT_GROUP/wimoor-ozon" "${tmp_file}"
  sed -i 's/product-write: false/product-write: true/g' "${tmp_file}"
  sed -i 's/stock-write: false/stock-write: true/g' "${tmp_file}"
  sed -i 's/price-write: false/price-write: true/g' "${tmp_file}"
  sed -i 's/posting-write: false/posting-write: true/g' "${tmp_file}"
  curl -sfS -X POST "${NACOS_HTTP_BASE}" \
    --data-urlencode "dataId=wimoor-ozon" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content@${tmp_file}" >/dev/null
  rm -f "${tmp_file}"
  sleep 5
}

ensure_sample_material() {
  local count
  count="$(mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" ${MYSQL_PASSWORD:+-p${MYSQL_PASSWORD}} -N -e \
    "SELECT COUNT(*) FROM db_erp.t_erp_material WHERE sku='${ERP_TEST_SKU}' AND shopid='${LOCAL_COMPANY_ID}'")"
  if [[ "${count}" != "0" ]]; then
    return 0
  fi
  local material_id
  material_id="$(python3 - <<'PY'
import time
print(int(time.time() * 1000))
PY
)"
  mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" ${MYSQL_PASSWORD:+-p${MYSQL_PASSWORD}} db_erp <<SQL
INSERT INTO t_erp_material (
  id, sku, name, shopid, owner, price, createdate, isDelete, issfg, color, mtype
) VALUES (
  ${material_id}, '${ERP_TEST_SKU}', 'Local Ozon Test Material', ${LOCAL_COMPANY_ID}, 0, 99.00, NOW(), b'0', '0', '0', 0
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  price = VALUES(price),
  isDelete = VALUES(isDelete);
SQL
}

ensure_sample_product_map() {
  local existing
  existing="$(mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" ${MYSQL_PASSWORD:+-p${MYSQL_PASSWORD}} -N -e \
    "SELECT COUNT(*) FROM db_ozon.t_ozon_product_map WHERE auth_id='${AUTH_ID}' AND material_sku='${ERP_TEST_SKU}'")"
  if [[ "${existing}" != "0" ]]; then
    return 0
  fi
  local map_id
  map_id="$(python3 - <<'PY'
import time
print(int(time.time() * 1000) + 7)
PY
)"
  mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u "${MYSQL_USER}" ${MYSQL_PASSWORD:+-p${MYSQL_PASSWORD}} db_ozon <<SQL
INSERT INTO t_ozon_product_map (
  id, auth_id, shop_id, material_sku, material_name, owner_name, image, material_price, ozon_offer_id, status, create_time, update_time
) VALUES (
  ${map_id}, ${AUTH_ID}, ${LOCAL_COMPANY_ID}, '${ERP_TEST_SKU}', 'Local Ozon Test Material', 'local-admin', NULL, 99.00, '${ERP_TEST_SKU}', 'MAPPED', NOW(), NOW()
)
ON DUPLICATE KEY UPDATE
  ozon_offer_id = VALUES(ozon_offer_id),
  status = VALUES(status),
  material_price = VALUES(material_price),
  update_time = NOW();
SQL
}

enable_local_write_flags
ensure_sample_material

AUTH_ID="${AUTH_ID:-$(load_auth_id_state)}"
AUTH_ID="${AUTH_ID:-$(discover_auth_id_db || true)}"

bind_response=""
if [[ -z "${AUTH_ID}" ]]; then
  bind_response="$(post_json "/api/v1/auth/bind" "{\"name\":\"local-ozon\",\"clientId\":\"${OZON_CLIENT_ID}\",\"apiKey\":\"${OZON_API_KEY}\"}" || true)"
fi

AUTH_ID="${AUTH_ID:-$(BIND_RESPONSE="${bind_response}" python3 - <<'PY'
import json
import os

raw = os.environ.get("BIND_RESPONSE", "")
if not raw:
    print("")
else:
    payload = json.loads(raw)
    print(((payload.get("data") or {}).get("id")) or "")
PY
)}"
AUTH_ID="${AUTH_ID:-$(discover_auth_id_direct "${OZON_DIRECT_BASE}" || true)}"
require_var AUTH_ID
save_auth_id_state "${AUTH_ID}"
if [[ -n "${bind_response}" ]]; then
  assert_success_json "${bind_response}"
fi

ensure_sample_product_map

if [[ -n "${WAREHOUSE_ID:-}" ]]; then
  response="$(post_json "/api/v1/stock/push" "{\"authId\":\"${AUTH_ID}\",\"warehouseId\":\"${WAREHOUSE_ID}\",\"items\":[{\"materialSku\":\"${ERP_TEST_SKU}\",\"quantity\":5}]}")"
  assert_success_json "${response}"
fi

response="$(post_json "/api/v1/price/push" "{\"authId\":\"${AUTH_ID}\",\"currencyCode\":\"RUB\",\"items\":[{\"materialSku\":\"${ERP_TEST_SKU}\",\"price\":99.00,\"oldPrice\":129.00}]}")"
assert_success_json "${response}"
response="$(post_json "/api/v1/posting/sync" "{\"authId\":\"${AUTH_ID}\",\"sinceDays\":7}")"
assert_success_json "${response}"

if [[ -n "${POSTING_ID:-}" ]]; then
  response="$(post_json "/api/v1/shipment/pushTracking" "{\"authId\":\"${AUTH_ID}\",\"postingId\":\"${POSTING_ID}\",\"trackingNumber\":\"TRACK-LOCAL-001\",\"deliveryService\":\"CDEK\"}")"
  assert_success_json "${response}"
fi

if [[ -n "${DRAFT_ID:-}" ]]; then
  response="$(post_json "/api/v1/product/publish" "{\"authId\":\"${AUTH_ID}\",\"draftId\":\"${DRAFT_ID}\"}")"
  assert_success_json "${response}"
fi

finance_report_id="local-fin-$(date +%Y%m%d%H%M%S)"
finance_report_date="$(date +%F)"
finance_payload="{\"transactions\":[{\"transactionId\":\"${finance_report_id}-1\",\"operationType\":\"sale\",\"postingNumber\":\"posting-local-1\",\"amount\":12.50,\"currencyCode\":\"RUB\",\"transactionTime\":\"${finance_report_date}T08:00:00Z\"}]}"
response="$(post_json "/api/v1/finance/import" "{\"authId\":\"${AUTH_ID}\",\"reportId\":\"${finance_report_id}\",\"reportDate\":\"${finance_report_date}\",\"rawContent\":$(python3 - <<PY
import json
print(json.dumps("""${finance_payload}"""))
PY
)}")"
assert_success_json "${response}"

chat_payload='{"sessions":[{"sessionId":"session-local-1","customerName":"Local Buyer","sessionStatus":"OPEN","messages":[{"messageId":"msg-local-1","senderType":"BUYER","messageText":"hello","messageTime":"2026-04-11T08:00:00Z","read":false}]}]}'
response="$(post_json "/api/v1/chat/import" "{\"authId\":\"${AUTH_ID}\",\"rawContent\":$(python3 - <<PY
import json
print(json.dumps("""${chat_payload}"""))
PY
)}")"
assert_success_json "${response}"
response="$(post_json "/api/v1/chat/reply/record" "{\"authId\":\"${AUTH_ID}\",\"sessionId\":\"session-local-1\",\"replyText\":\"local reply audit\"}")"
assert_success_json "${response}"

ads_payload='{"account":{"accountId":"acc-local-1","accountName":"Local Ads Account","status":"ACTIVE","currencyCode":"RUB"},"campaigns":[{"campaignId":"camp-local-1","campaignName":"Local Campaign","campaignType":"SEARCH_PROMO","campaignStatus":"ACTIVE","budget":1000}],"reports":[{"campaignId":"camp-local-1","reportDate":"2026-04-11","impressions":1000,"clicks":50,"spend":120.5,"orders":5,"sales":800,"ctr":5,"cpc":2.41,"acos":15.06,"roas":6.64}]}'
response="$(post_json "/api/v1/ads/import" "{\"authId\":\"${AUTH_ID}\",\"rawContent\":$(python3 - <<PY
import json
print(json.dumps("""${ads_payload}"""))
PY
)}")"
assert_success_json "${response}"

echo "Write smoke completed."
