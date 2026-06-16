#!/usr/bin/env bash
set -euo pipefail

SERVER_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "${SERVER_SCRIPT_DIR}/../local/_common.sh"

load_local_env

ARTIFACT_ROOT="${ARTIFACT_ROOT:-${REPO_ROOT}/.deploy/ozon}"
CONFIG_DIR="${CONFIG_DIR:-${ARTIFACT_ROOT}/config}"
OZON_PORT="${OZON_PORT:-8106}"
ERP_DISCOVERY_URI="${ERP_DISCOVERY_URI:-http://127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

mkdir -p "${CONFIG_DIR}"

cat > "${CONFIG_DIR}/application.yml" <<EOF
server:
  address: 0.0.0.0
  port: ${OZON_PORT}
  servlet:
    context-path: /ozon
spring:
  application:
    name: wimoor-ozon
  mvc:
    pathmatch:
      matching-strategy: ant_path_matcher
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
    discovery:
      client:
        simple:
          instances:
            wimoor-erp:
              - uri: ${ERP_DISCOVERY_URI}
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    jdbc-url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/db_ozon?allowMultiQueries=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
    primary:
      driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    database: 0
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    timeout: 60000
ozon:
  api:
    base-url: https://api-seller.ozon.ru
  security:
    aes-key: ${OZON_SECURITY_AES_KEY}
  feature:
    auth: true
    product: true
    product-write: ${OZON_PRODUCT_WRITE:-false}
    task: true
    error: true
    finance: true
    chat: true
    ads: true
    stock-write: ${OZON_STOCK_WRITE:-false}
    price-write: ${OZON_PRICE_WRITE:-false}
    posting-write: ${OZON_POSTING_WRITE:-false}
    chat-send: false
    ads-sync: false
config:
  photo-server: 127.0.0.1
  photo-server-url: http://127.0.0.1
EOF

echo "Rendered ${CONFIG_DIR}/application.yml"
