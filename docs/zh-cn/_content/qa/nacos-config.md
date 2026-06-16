# Nacos 配置

## 导入方式

将 `init-config/nacos/DEFAULT_GROUP` 压缩为 zip 后导入 Nacos，或按文件逐个创建配置。

## 必须确认

- `wimoor-gateway` 路由配置存在。
- 各服务配置名称与 `spring.application.name` 一致。
- MySQL、Redis、Seata、外部平台配置已按环境替换。
- 示例密钥不能直接用于生产。

## 配置不生效

优先检查 active profile、Nacos 地址、namespace/group、服务名和配置格式。

