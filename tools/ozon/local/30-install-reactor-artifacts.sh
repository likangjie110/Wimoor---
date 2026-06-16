#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SCRIPT_DIR}/_common.sh"

load_local_env
ensure_command mvn

mvn -Dmaven.repo.local=/tmp/m2 install:install-file \
  -Dfile="${REPO_ROOT}/wimoor-erp/erp-boot/src/main/resources/lib/ocean.client.java.biz.jar" \
  -DgroupId=com.biz \
  -DartifactId=biz \
  -Dversion=1.0 \
  -Dpackaging=jar

mvn -Dmaven.repo.local=/tmp/m2 install:install-file \
  -Dfile="${REPO_ROOT}/wimoor-erp/erp-boot/src/main/resources/lib/aop-sdk-message-0.9.0.jar" \
  -DgroupId=alibaba.message \
  -DartifactId=alibaba.message \
  -Dversion=0.9.0 \
  -Dpackaging=jar

mvn -Dmaven.repo.local=/tmp/m2 \
  -pl wimoor-admin/admin-boot,wimoor-gateway,wimoor-erp/erp-boot,wimoor-ozon/ozon-boot \
  -am -DskipTests clean install

for artifact in \
  /tmp/m2/com/wimoor/common-core/2.0.0/common-core-2.0.0.jar \
  /tmp/m2/com/wimoor/common-feishu/2.0.0/common-feishu-2.0.0.jar \
  /tmp/m2/com/wimoor/admin-api/2.0.0/admin-api-2.0.0.jar \
  /tmp/m2/com/biz/biz/1.0/biz-1.0.jar \
  /tmp/m2/alibaba/message/alibaba.message/0.9.0/alibaba.message-0.9.0.jar \
  /tmp/m2/com/wimoor/erp-api/2.0.0/erp-api-2.0.0.jar \
  /tmp/m2/com/wimoor/ozon-api/2.0.0/ozon-api-2.0.0.jar \
  /tmp/m2/com/wimoor/ozon-boot/2.0.0/ozon-boot-2.0.0.jar
do
  [[ -f "${artifact}" ]] || { echo "Missing artifact: ${artifact}" >&2; exit 1; }
done

echo "Backend reactor artifacts installed into /tmp/m2."
