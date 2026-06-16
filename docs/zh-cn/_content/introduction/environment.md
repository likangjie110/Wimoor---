# 环境要求

## 基础环境

| 类型 | 建议 |
| --- | --- |
| JDK | JDK 1.8 |
| Maven | Maven 3.2.3+ |
| Node.js | 与当前 `wimoorui/package.json` 依赖兼容的 Node 版本 |
| MySQL | MySQL 8.0 |
| Redis | 用于登录会话和缓存 |
| Nacos | 配置中心和服务发现 |
| Seata | 分布式事务，按业务需要启用 |

## 本地依赖

- MySQL 结构和基础数据位于 `init-config/mysql`。
- Nacos 配置模板位于 `init-config/nacos/DEFAULT_GROUP`。
- Seata 配置模板位于 `init-config/seata`。
- 前端依赖由 `wimoorui/package.json` 管理。

## 启动前检查

1. 确认 MySQL、Redis、Nacos 可访问。
2. 导入业务库结构和基础数据。
3. 将 `init-config/nacos/DEFAULT_GROUP` 中配置导入 Nacos。
4. 确认本地端口未被占用，尤其是 `8099`、`8100` 到 `8107`、`8084`。
5. 处理 `wimoor-finance` 与 `wimoor-ozon` 当前同为 `8106` 的端口冲突。

