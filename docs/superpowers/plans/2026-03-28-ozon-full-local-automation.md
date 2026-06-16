# Wimoor Ozon Full Delivery And Local Automation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐 `wimoor-ozon` 的剩余本地可用性缺口，并提供一套可重复执行的本地安装、启动、测试、验收自动化方案。

**Architecture:** 继续沿用当前 `wimoor-ozon` 独立 bounded context，不重构 Amazon 现有链路。自动化分为四层：代码补口、基础设施导入、应用安装启动、读写冒烟验收。所有本地敏感信息通过本地环境变量或本地私有 env 文件注入，不写入仓库。

**Tech Stack:** Maven multi-module, Spring Boot 2.6, Java 8, MySQL 8, Redis, Nacos 2.3.x, Seata 1.6.x, Vue 3 + Vite, Bash/Node helper scripts.

---

## Verified Baseline

以下内容已在当前仓库中拿到 fresh evidence，可直接作为执行起点：

- `timeout 900s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests,OzonProductMetadataServiceTests,OzonProductPreviewServiceTests,OzonProductPublishServiceTests,OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test`
  - 已通过
- `node wimoorui/scripts/check_ozon_entry.mjs`
  - 已通过
- `node wimoorui/scripts/check_ozon_product_publish_entry.mjs`
  - 已通过
- `cd wimoorui && timeout 900s ./scripts/build_in_linux_fs.sh`
  - 已通过
- `timeout 1800s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon/amazon-boot -am -DskipTests clean package`
  - 已通过
- `timeout 1800s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon-adv/amazon-adv-boot -am -DskipTests clean package`
  - 已通过
- `timeout 1800s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-ozon/ozon-boot -am -DskipTests install`
  - 已通过

以下阻塞点也已被当前会话验证，必须写入自动化方案：

- 使用 `mvn -Dmaven.repo.local=/tmp/m2 -f wimoor-ozon/ozon-boot/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev` 启动 `ozon-boot` 时，日志系统会尝试写入 `/logs/wimoor-ozon/log.log`，当前环境报 `只读文件系统`
- 在修复 `LOG_HOME` 之后，Nacos client 还会尝试写 `${user.home}/logs/nacos/*.log`；本地启动脚本必须同时注入 `JM.LOG.PATH` 和 `JM.SNAPSHOT.PATH`
- 在修复两层日志路径之后，如果 Nacos 未导入 `wimoor-common / wimoor-commom-ext / wimoor-ozon`，启动会因缺少 `config.photo-server-url` 之类配置而失败
- 在 Nacos 配置补齐之后，如果 Seata 未启动，启动会继续报 `127.0.0.1:8091` 连接拒绝
- `wimoorui/vite.config.js` 当前 `server.proxy` 未包含 `/ozon/api`，本地前端 dev 模式下 Ozon API 会缺少代理路径

## File Structure Map

### Existing files to modify

- Modify: `wimoor-common/common-core/src/main/resources/logback-spring.xml`
- Modify: `wimoorui/vite.config.js`
- Modify: `docs/superpowers/plans/2026-03-28-ozon-delivery-automation.md`

### New automation entrypoints

- Create: `tools/ozon/local/00-env.example`
- Create: `tools/ozon/local/10-prepare-mysql.sh`
- Create: `tools/ozon/local/15-bootstrap-infra.sh`
- Create: `tools/ozon/local/20-import-nacos.sh`
- Create: `tools/ozon/local/25-start-infra.sh`
- Create: `tools/ozon/local/30-install-reactor-artifacts.sh`
- Create: `tools/ozon/local/40-start-backend.sh`
- Create: `tools/ozon/local/50-start-frontend.sh`
- Create: `tools/ozon/local/60-smoke-readonly.sh`
- Create: `tools/ozon/local/65-smoke-gateway.sh`
- Create: `tools/ozon/local/70-smoke-write.sh`
- Create: `tools/ozon/local/80-stop-local-stack.sh`
- Create: `tools/ozon/local/90-full-check.sh`

### Existing reference files to read, not rewrite

- Read: `README.md`
- Read: `init-config/mysql/readme.txt`
- Read: `init-config/nacos/install.txt`
- Read: `init-config/seata/readme.txt`
- Read: `init-config/nacos/DEFAULT_GROUP/wimoor-common`
- Read: `init-config/nacos/DEFAULT_GROUP/wimoor-ozon`
- Read: `wimoor-admin/admin-boot/src/main/resources/bootstrap-dev.yml`
- Read: `wimoor-gateway/src/main/resources/bootstrap-dev.yml`
- Read: `wimoor-erp/erp-boot/src/main/resources/bootstrap-dev.yml`
- Read: `wimoor-ozon/ozon-boot/src/main/resources/bootstrap-dev.yml`

## Chunk 1: Close Local Runtime Blockers

### Task 1: Make local log output path writable and configurable

**Files:**
- Modify: `wimoor-common/common-core/src/main/resources/logback-spring.xml`

- [ ] **Step 1: Reproduce the current startup failure**

Run:

```bash
timeout 180s mvn -Dmaven.repo.local=/tmp/m2 -f wimoor-ozon/ozon-boot/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: FAIL with `openFile(/logs/wimoor-ozon/log.log,true)` and `只读文件系统`.

- [ ] **Step 2: Replace hardcoded log root with env-overridable path**

Change:

```xml
<property name="LOG_HOME" value="/logs/${APP_NAME}" />
```

to an env-overridable local-safe form, for example:

```xml
<property name="LOG_HOME" value="${LOG_HOME:-/logs/${APP_NAME}}" />
```

and document local default usage:

```bash
export LOG_HOME=/tmp/wimoor-logs/wimoor-ozon
mkdir -p "${LOG_HOME}"
export JM_LOG_PATH=/tmp/wimoor-logs/nacos
export JM_SNAPSHOT_PATH=/tmp/wimoor-nacos-snapshot
mkdir -p "${JM_LOG_PATH}" "${JM_SNAPSHOT_PATH}"
```

- [ ] **Step 3: Re-run startup to verify the log path blocker is gone**

Run:

```bash
mkdir -p /tmp/wimoor-logs/wimoor-ozon
mkdir -p /tmp/wimoor-logs/nacos /tmp/wimoor-nacos-snapshot
env \
  LOG_HOME=/tmp/wimoor-logs/wimoor-ozon \
  JM_LOG_PATH=/tmp/wimoor-logs/nacos \
  JM_SNAPSHOT_PATH=/tmp/wimoor-nacos-snapshot \
  'JM.LOG.PATH=/tmp/wimoor-logs/nacos' \
  'JM.SNAPSHOT.PATH=/tmp/wimoor-nacos-snapshot' \
  timeout 180s mvn -Dmaven.repo.local=/tmp/m2 -f wimoor-ozon/ozon-boot/pom.xml -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: if still failing, it must fail on the next real dependency blocker, not `/logs/...`.

- [ ] **Step 4: Confirm the next blocker sequence**

After the two log-path overrides are in place:

```text
If Nacos config is missing -> expect unresolved placeholders such as config.photo-server-url
If Nacos config is present but Seata is absent -> expect 127.0.0.1:8091 connection refused
```

### Task 2: Add Ozon frontend dev proxy

**Files:**
- Modify: `wimoorui/vite.config.js`

- [ ] **Step 1: Reproduce the missing proxy gap**

Inspect:

```bash
rg -n "'/ozon/api'|\"/ozon/api\"" wimoorui/vite.config.js
```

Expected: no results.

- [ ] **Step 2: Add Ozon proxy to gateway**

Add:

```js
'/ozon/api': serverurl,
```

under `server.proxy`.

- [ ] **Step 3: Verify the proxy entry exists**

Run:

```bash
rg -n "'/ozon/api'|\"/ozon/api\"" wimoorui/vite.config.js
```

Expected: one proxy entry found.

## Chunk 2: Create Local Automation Entry Points

### Task 3: Add local env template and secret boundary

**Files:**
- Create: `tools/ozon/local/00-env.example`

- [ ] **Step 1: Create a repo-local env template**

Template must include these keys:

```bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=wimoor_local
MYSQL_PASSWORD=change_me
MYSQL_BOOTSTRAP_SOCKET_USER=root
REDIS_HOST=127.0.0.1
REDIS_PASSWORD=change_me
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=public
NACOS_IP=127.0.0.1
LOG_HOME=/tmp/wimoor-logs
JM_LOG_PATH=/tmp/wimoor-logs/nacos
JM_SNAPSHOT_PATH=/tmp/wimoor-nacos-snapshot
LOCAL_USER_ID=local-admin
LOCAL_COMPANY_ID=company-1
# Non-numeric placeholders are acceptable; _common.sh will resolve the first numeric shop id from db_admin when needed.
OZON_SECURITY_AES_KEY=change_me_16_or_32_bytes
OZON_CLIENT_ID=fill_locally
OZON_API_KEY=fill_locally
```

- [ ] **Step 2: Explicitly forbid committing local secrets**

Document in the file header:

```text
Do not place real Client ID / API Key in git-tracked files.
Copy this file to an ignored local env file before use.
```

### Task 4: Automate MySQL schema and seed import

**Files:**
- Create: `tools/ozon/local/10-prepare-mysql.sh`

- [ ] **Step 1: Write a dry-run mode**

The script must support:

```bash
bash tools/ozon/local/10-prepare-mysql.sh --check
```

and print which databases or SQL folders are missing.

- [ ] **Step 2: Implement exact import order**

Import at least:

```text
init-config/mysql/数据库结构/seata
init-config/mysql/数据库结构/db_quartz
init-config/mysql/数据库结构/db_admin
init-config/mysql/数据/db_admin
init-config/mysql/数据库结构/db_erp
init-config/mysql/数据/db_erp
init-config/mysql/数据库结构/db_ozon
```

Optional but recommended for full repo compatibility:

```text
init-config/mysql/数据库结构/db_amazon
init-config/mysql/数据/db_amazon
init-config/mysql/数据库结构/db_amazon_adv
init-config/mysql/数据/db_amazon_adv
```

- [ ] **Step 3: Verify import script completes**

Run:

```bash
bash tools/ozon/local/10-prepare-mysql.sh
```

Expected: zero shell errors and all target databases present.

- [ ] **Step 4: Bootstrap one local TCP app user**

Use socket login as bootstrap root, then create:

```text
${MYSQL_USER}@localhost
${MYSQL_USER}@127.0.0.1
```

and grant them access to `db_admin / db_erp / db_ozon / db_quartz / seata`.

### Task 5: Automate Nacos config import

**Files:**
- Create: `tools/ozon/local/20-import-nacos.sh`

- [ ] **Step 1: Import required data IDs**

At minimum:

```text
wimoor-common
wimoor-commom-ext
wimoor-admin
wimoor-gateway
wimoor-erp
wimoor-ozon
seataServer.properties
```

- [ ] **Step 2: Use a deterministic API-based import**

Implement a loop around:

```bash
curl -sfS -X POST "http://${NACOS_SERVER_ADDR}/nacos/v1/cs/configs" \
  --data-urlencode "dataId=${DATA_ID}" \
  --data-urlencode "group=DEFAULT_GROUP" \
  --data-urlencode "content@${FILE_PATH}" \
  --data-urlencode "type=${FILE_TYPE}"
```

- [ ] **Step 3: Verify config visibility**

Run:

```bash
bash tools/ozon/local/20-import-nacos.sh --check
```

Expected: all required data IDs reported as present.

### Task 5A: Automate local Nacos and Seata bootstrap

**Files:**
- Create: `tools/ozon/local/15-bootstrap-infra.sh`
- Create: `tools/ozon/local/25-start-infra.sh`

- [ ] **Step 1: Download exact archive versions**

Use:

```text
Nacos 2.3.0
Seata 1.6.1
```

- [ ] **Step 2: Extract with owner stripping**

Run:

```bash
tar --no-same-owner -xf /tmp/wimoor-ozon-local/nacos-server-2.3.0.tar.gz -C /tmp/wimoor-ozon-local
tar --no-same-owner -xf /tmp/wimoor-ozon-local/seata-server-1.6.1.tar.gz -C /tmp/wimoor-ozon-local
```

Expected: extraction succeeds without `Cannot change ownership to uid 502` errors.

- [ ] **Step 3: Start Nacos and wait for 8848**

Expected: local `127.0.0.1:8848` becomes reachable.

- [ ] **Step 4: Import required Nacos configs**

Run `20-import-nacos.sh` after Nacos is reachable.

- [ ] **Step 5: Start Seata and wait for 8091**

Expected: local `127.0.0.1:8091` becomes reachable.

### Task 6: Automate reactor artifact installation

**Files:**
- Create: `tools/ozon/local/30-install-reactor-artifacts.sh`

- [ ] **Step 1: Install only the Ozon dependency chain**

Use a backend-stack install command, not only `ozon-boot`:

```bash
mvn -Dmaven.repo.local=/tmp/m2 install:install-file \
  -Dfile=wimoor-erp/erp-boot/src/main/resources/lib/ocean.client.java.biz.jar \
  -DgroupId=com.biz -DartifactId=biz -Dversion=1.0 -Dpackaging=jar

mvn -Dmaven.repo.local=/tmp/m2 install:install-file \
  -Dfile=wimoor-erp/erp-boot/src/main/resources/lib/aop-sdk-message-0.9.0.jar \
  -DgroupId=alibaba.message -DartifactId=alibaba.message -Dversion=0.9.0 -Dpackaging=jar

mvn -Dmaven.repo.local=/tmp/m2 \
  -pl wimoor-admin/admin-boot,wimoor-gateway,wimoor-erp/erp-boot,wimoor-ozon/ozon-boot \
  -am -DskipTests clean install
```

- [ ] **Step 2: Verify local artifacts exist**

Check:

```bash
test -f /tmp/m2/com/wimoor/common-core/2.0.0/common-core-2.0.0.jar
test -f /tmp/m2/com/wimoor/common-feishu/2.0.0/common-feishu-2.0.0.jar
test -f /tmp/m2/com/wimoor/admin-api/2.0.0/admin-api-2.0.0.jar
test -f /tmp/m2/com/biz/biz/1.0/biz-1.0.jar
test -f /tmp/m2/alibaba/message/alibaba.message/0.9.0/alibaba.message-0.9.0.jar
test -f /tmp/m2/com/wimoor/erp-api/2.0.0/erp-api-2.0.0.jar
test -f /tmp/m2/com/wimoor/ozon-api/2.0.0/ozon-api-2.0.0.jar
test -f /tmp/m2/com/wimoor/ozon-boot/2.0.0/ozon-boot-2.0.0.jar
```

Expected: all files exist.

## Chunk 3: Automate Application Start And Stop

### Task 7: Automate backend startup order

**Files:**
- Create: `tools/ozon/local/40-start-backend.sh`
- Create: `tools/ozon/local/80-stop-local-stack.sh`

- [ ] **Step 1: Start minimal support stack**

The script must assume MySQL/Redis/Nacos/Seata are already available, then start:

```text
wimoor-admin/admin-boot  -> 8100 /admin
wimoor-gateway           -> 8099
wimoor-erp/erp-boot      -> 8101 /erp
wimoor-ozon/ozon-boot    -> 8106 /ozon
```

Use:

```bash
mvn -Dmaven.repo.local=/tmp/m2 -f <module-pom> -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev
```

and redirect logs to `/tmp/wimoor-logs/<service>.log`.

- [ ] **Step 2: Add health waiters**

For each service, poll the expected port and fail fast if the JVM exits.

- [ ] **Step 3: Add clean stop logic**

`80-stop-local-stack.sh` must stop only the processes started by the startup script.

### Task 8: Automate frontend startup

**Files:**
- Create: `tools/ozon/local/50-start-frontend.sh`

- [ ] **Step 1: Install frontend dependencies**

Use:

```bash
cd wimoorui
npm install
```

- [ ] **Step 2: Start Vite against gateway**

Verified local assumption from `wimoorui/vite.config.js`:

```text
dev server port: 8084
gateway target: http://localhost:8099
```

- [ ] **Step 3: Verify UI starts**

Expected: Vite listens on `http://127.0.0.1:8084` and Ozon API requests are proxied through gateway.

## Chunk 4: Automate Ozon Smoke Verification

### Task 9: Automate read-only smoke

**Files:**
- Create: `tools/ozon/local/60-smoke-readonly.sh`

- [ ] **Step 1: Read auth list and connectivity**

Build one reusable direct-call header:

```bash
export OZON_DIRECT_BASE="http://127.0.0.1:8106/ozon"
export X_USERINFO="$(python3 - <<'PY'
import json, os, urllib.parse
payload = {"id": os.environ["LOCAL_USER_ID"], "companyid": os.environ["LOCAL_COMPANY_ID"]}
print(urllib.parse.quote(json.dumps(payload, ensure_ascii=False)))
PY
)"
```

```bash
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/auth/list"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/auth/ping?authId=${AUTH_ID}"
```

- [ ] **Step 2: Read product workbench data**

```bash
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/product/draft/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/product/category/tree?authId=${AUTH_ID}"
```

- [ ] **Step 3: Read operational domains**

```bash
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/stock/snapshot/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/price/snapshot/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/posting/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/task/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/error/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/finance/task/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/chat/session/list?authId=${AUTH_ID}"
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/ads/summary?authId=${AUTH_ID}"
```

- [ ] **Step 4: Verify product publish read flow**

```bash
curl -sfS -H "X-USERINFO: ${X_USERINFO}" "${OZON_DIRECT_BASE}/api/v1/product/draft/detail?authId=${AUTH_ID}&draftId=${DRAFT_ID}"
curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/product/preview" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"draftId\":\"${DRAFT_ID}\"}"
```

### Task 10: Automate write smoke with local secrets

**Files:**
- Create: `tools/ozon/local/70-smoke-write.sh`

- [ ] **Step 1: Reuse existing local auth or bind one auth without committing secrets**

Use:

```bash
curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/auth/bind" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"local-ozon\",\"clientId\":\"${OZON_CLIENT_ID}\",\"apiKey\":\"${OZON_API_KEY}\"}"
```

Expected script behavior:

```text
1. reuse AUTH_ID from .env.local or .state when present
2. else query db_ozon.t_ozon_auth by OZON_CLIENT_ID
3. call /api/v1/auth/bind only when no local auth exists
```

- [ ] **Step 2: Enable write feature flags only for local smoke**

Temporarily set in local Nacos:

```text
ozon.feature.product-write=true
ozon.feature.stock-write=true
ozon.feature.price-write=true
ozon.feature.posting-write=true
```

Keep `ozon.feature.chat-send` and `ozon.feature.ads-sync` disabled until the upstream contract is separately verified.

- [ ] **Step 3: Bootstrap sample ERP data when the local DB is empty**

Before issuing write APIs, ensure local fixture rows exist for `ERP-SKU-1`:

```text
db_erp.t_erp_material.sku = ERP-SKU-1
db_ozon.t_ozon_product_map.material_sku = ERP-SKU-1
```

This avoids false negatives caused by an empty local ERP catalog or missing product map rather than Ozon write logic.

- [ ] **Step 4: Run minimal write smoke**

Examples:

```bash
curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/stock/push" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"warehouseId\":\"${WAREHOUSE_ID}\",\"items\":[{\"materialSku\":\"ERP-SKU-1\",\"quantity\":5}]}"

curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/price/push" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"currencyCode\":\"RUB\",\"items\":[{\"materialSku\":\"ERP-SKU-1\",\"price\":99.00,\"oldPrice\":129.00}]}"

curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/posting/sync" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"sinceDays\":7}"

curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/shipment/pushTracking" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"postingId\":\"${POSTING_ID}\",\"trackingNumber\":\"TRACK-LOCAL-001\",\"deliveryService\":\"CDEK\"}"

curl -sfS -X POST "${OZON_DIRECT_BASE}/api/v1/product/publish" \
  -H "X-USERINFO: ${X_USERINFO}" \
  -H "Content-Type: application/json" \
  -d "{\"authId\":\"${AUTH_ID}\",\"draftId\":\"${DRAFT_ID}\"}"
```

`WAREHOUSE_ID`, `POSTING_ID`, and `DRAFT_ID` are optional local inputs. When they are absent, the script should still complete the available write paths and record the skipped branches in the final report.

- [ ] **Step 5: Validate write results**

Expected:

```text
product publish returns localTaskId / remoteTaskId / taskStatus
task detail can be queried
stock/price/posting/shipment writes produce success or explicit business error
no endpoint performs real write when write flag is false
```

- [ ] **Step 6: Add browser-level gateway smoke**

After backend direct smoke is green, verify browser routing through Vite + gateway:

```text
Vite: http://127.0.0.1:8084
Gateway: http://127.0.0.1:8099
Ozon service: http://127.0.0.1:8106/ozon
```

Use `tools/ozon/local/65-smoke-gateway.sh` to:

1. `POST http://127.0.0.1:8100/admin/api/v1/auth/login`
2. Parse `data.session` as `jsessionid`
3. `GET http://127.0.0.1:8099/ozon/api/v1/auth/list`

## Chunk 5: Final Automation Gate

### Task 11: Create one-button verification wrapper

**Files:**
- Create: `tools/ozon/local/90-full-check.sh`

- [ ] **Step 1: Chain verified build/test commands**

The wrapper must run, in order:

```bash
timeout 900s mvn -Dmaven.repo.local=/tmp/m2 -DfailIfNoTests=false -pl wimoor-ozon/ozon-boot -am -Dtest=OzonListingDraftServiceTests,OzonProductMetadataServiceTests,OzonProductPreviewServiceTests,OzonProductPublishServiceTests,OzonProductControllerFeatureTests,OzonSmokeWorkflowTests test
node wimoorui/scripts/check_ozon_entry.mjs
node wimoorui/scripts/check_ozon_product_publish_entry.mjs
cd wimoorui && timeout 900s ./scripts/build_in_linux_fs.sh
timeout 1800s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon/amazon-boot -am -DskipTests clean package
timeout 1800s mvn -Dmaven.repo.local=/tmp/m2 -pl wimoor-amazon-adv/amazon-adv-boot -am -DskipTests clean package
```

- [ ] **Step 2: Add local startup + smoke hooks**

After build/test pass:

```bash
bash tools/ozon/local/15-bootstrap-infra.sh
bash tools/ozon/local/25-start-infra.sh
bash tools/ozon/local/30-install-reactor-artifacts.sh
bash tools/ozon/local/40-start-backend.sh
bash tools/ozon/local/50-start-frontend.sh
bash tools/ozon/local/65-smoke-gateway.sh
bash tools/ozon/local/60-smoke-readonly.sh
bash tools/ozon/local/70-smoke-write.sh
```

- [ ] **Step 3: Emit machine-readable report**

Write one markdown report under:

```text
docs/superpowers/verification/ozon-local-report-YYYYMMDD-HHMM.md
```

including:

```text
git status snapshot
executed commands
pass/fail summary
blocked steps
local URLs
authId / draftId / taskId used for smoke
```

## Completion Criteria

This plan is only considered done when all of the following are true:

- `wimoor-ozon` feature matrix is fully green in the existing delivery runbook
- local startup no longer fails on `/logs/...` write permissions
- frontend dev can access Ozon APIs through `/ozon/api` proxy
- one-click install/start/smoke scripts exist and are repeatable
- gateway smoke passes with `jsessionid`
- readonly smoke passes
- write smoke passes under local write flags
- Ozon validated backend suite passes
- frontend build passes
- Amazon and Amazon-Adv regression packages pass

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-03-28-ozon-full-local-automation.md`. Ready to execute?
