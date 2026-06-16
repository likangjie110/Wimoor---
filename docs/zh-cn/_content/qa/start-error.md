# 启动报错

## 先看哪里

1. 当前服务的 `bootstrap.yml` 和 profile 文件。
2. Nacos 对应服务配置。
3. MySQL、Redis、Nacos、Seata 是否启动。
4. 端口是否被占用。
5. 依赖版本是否与 JDK/Maven 匹配。

## 常见原因

- 未导入 Nacos 配置。
- 未导入数据库结构。
- 数据库账号或连接串错误。
- 服务名与网关路由不一致。
- Ozon 与 Finance 同端口启动。

