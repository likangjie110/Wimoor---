# 敏感配置脱敏

文档、截图和问题反馈中不得出现真实凭据。

## 必须脱敏的键

- `password`
- `secret`
- `token`
- `key`
- `access_key`
- `app-id`
- `appSecret`
- 邮箱、短信、微信、飞书、DeepSeek、Amazon、Ozon 等第三方凭据

## 写法

```properties
spring.datasource.password=<redacted>
aliyun.sms.access_key_secret=<redacted>
deepseek.token=<redacted>
```

## 原则

只保留配置项含义和使用位置，不保留真实值。示例值也按敏感值处理。

