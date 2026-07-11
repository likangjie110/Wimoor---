# Phase 5 API 集成指南

## OZON Seller API 端点参考

### 需要验证的 API 端点

以下 API 端点基于常见的 OZON Seller API 模式推测，**需要根据官方文档验证**：

#### 1. 取消订单 API
- **当前使用**: `/v2/posting/fbs/cancel`
- **需确认**:
  - 端点路径是否正确
  - `cancel_reason_id` 的有效值列表
  - 是否支持批量取消

**官方文档链接**: https://docs.ozon.ru/api/seller/

#### 2. 获取包裹信息 API
- **当前使用**: `/v1/posting/fbs/package`
- **需确认**:
  - 端点路径是否正确
  - 响应数据结构
  - 是否支持批量查询

#### 3. 获取退货信息 API
- **当前使用**: `/v3/returns/company/fbs`
- **需确认**:
  - 端点路径是否正确
  - 查询参数（posting_number vs filter）
  - 分页支持

---

## API 端点验证步骤

### 步骤 1: 查阅官方文档

访问 OZON Seller API 官方文档：
- 主文档: https://docs.ozon.ru/api/seller/
- API 参考: https://api-seller.ozon.ru/schema/

### 步骤 2: 使用 Postman 测试

#### 准备测试环境

1. 获取测试授权凭证
   - Client-Id
   - Api-Key

2. 配置 Postman 环境变量
   ```json
   {
     "base_url": "https://api-seller.ozon.ru",
     "client_id": "YOUR_CLIENT_ID",
     "api_key": "YOUR_API_KEY"
   }
   ```

#### 测试取消订单 API

```http
POST {{base_url}}/v2/posting/fbs/cancel
Headers:
  Client-Id: {{client_id}}
  Api-Key: {{api_key}}
  Content-Type: application/json

Body:
{
  "posting_number": "TEST-POSTING-001",
  "cancel_reason_id": 352,
  "cancel_reason_message": "测试取消"
}
```

**预期响应**:
```json
{
  "result": {
    "posting_number": "TEST-POSTING-001",
    "status": "cancelled"
  }
}
```

#### 测试获取包裹信息 API

```http
POST {{base_url}}/v1/posting/fbs/package
Headers:
  Client-Id: {{client_id}}
  Api-Key: {{api_key}}
  Content-Type: application/json

Body:
{
  "posting_number": "TEST-POSTING-001"
}
```

#### 测试获取退货信息 API

```http
POST {{base_url}}/v3/returns/company/fbs
Headers:
  Client-Id: {{client_id}}
  Api-Key: {{api_key}}
  Content-Type: application/json

Body:
{
  "posting_number": "TEST-POSTING-001"
}
```

### 步骤 3: 更新代码

如果 API 端点不正确，更新以下文件：

1. `DefaultOzonSellerApiClient.java` - 更新端点常量
2. `OzonAfterSaleServiceImpl.java` - 调整请求/响应处理逻辑
3. 相关测试文件

---

## 取消原因码 (cancel_reason_id)

### 常见取消原因码

需要从 OZON 官方文档获取完整列表，以下是推测的常见值：

| 原因码 | 说明 | 适用场景 |
|--------|------|---------|
| 352 | 客户要求取消 | 买家主动取消 |
| 400 | 商品缺货 | 卖家无法履约 |
| 401 | 地址错误 | 无法配送 |
| 402 | 价格错误 | 卖家定价失误 |

**待办**: 从官方文档获取完整的 `cancel_reason_id` 列表并创建枚举类。

---

## 错误处理

### 常见错误场景

#### 1. 认证失败 (401)
```json
{
  "code": 401,
  "message": "Invalid credentials",
  "details": []
}
```

**处理**: 检查 Client-Id 和 Api-Key 是否正确

#### 2. 资源不存在 (404)
```json
{
  "code": 404,
  "message": "Posting not found",
  "details": []
}
```

**处理**: 验证 posting_number 是否正确

#### 3. 业务规则限制 (400)
```json
{
  "code": 400,
  "message": "Cannot cancel posting in current status",
  "details": []
}
```

**处理**: 检查订单状态是否允许取消

#### 4. 频率限制 (429)
```json
{
  "code": 429,
  "message": "Too many requests",
  "details": []
}
```

**处理**: 实施重试机制和限流策略

---

## 测试数据

### 测试店铺信息

**待提供**: 配置测试环境的店铺授权信息

```properties
# 测试环境配置
ozon.test.client-id=YOUR_TEST_CLIENT_ID
ozon.test.api-key=YOUR_TEST_API_KEY
ozon.test.posting-number=TEST-POSTING-001
```

### 测试订单

创建测试订单数据：

```sql
-- 插入测试 Posting
INSERT INTO t_ozon_posting (
  id, auth_id, shop_id, posting_number,
  fulfillment_type, posting_status,
  order_created_at, create_time, update_time
) VALUES (
  'TEST-POSTING-001',
  'TEST-AUTH-001',
  'TEST-SHOP-001',
  '12345678-0001-1',
  'FBS',
  'awaiting_packaging',
  NOW(),
  NOW(),
  NOW()
);
```

---

## API 调用监控

### 日志查询

查询 API 调用日志：

```sql
-- 查看售后 API 调用记录
SELECT
  api_name,
  request_payload,
  response_payload,
  status,
  error_message,
  elapsed_millis,
  create_time
FROM t_ozon_api_log
WHERE api_group = 'AFTERSALE'
  AND create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY create_time DESC
LIMIT 100;
```

### 操作审计查询

查询售后操作审计：

```sql
-- 查看售后操作记录
SELECT
  operation_type,
  object_type,
  object_code,
  request_payload,
  result_status,
  result_message,
  operator,
  create_time
FROM t_ozon_operation_audit
WHERE operation_type LIKE 'AFTERSALE%'
  AND create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)
ORDER BY create_time DESC
LIMIT 100;
```

---

## 性能优化建议

### 1. 批量 API 调用

当前实现是单个订单调用，如果需要处理大量订单，考虑实现批量 API：

```java
public void syncPackagesBatch(List<String> postingIds) {
    // 批量查询包裹信息
    // 减少 API 调用次数
}
```

### 2. 缓存策略

对于变化不频繁的数据（如取消原因码），考虑缓存：

```java
@Cacheable(value = "ozon:cancel:reasons")
public List<CancelReason> getCancelReasons() {
    // 缓存取消原因列表
}
```

### 3. 异步处理

对于耗时的同步操作，考虑异步执行：

```java
@Async
public CompletableFuture<Void> syncPackagesAsync(String authId, String postingId) {
    // 异步同步包裹信息
}
```

---

## 安全注意事项

### 1. API Key 安全

- ✅ API Key 已加密存储
- ✅ 使用 `OzonCredentialService` 解密
- ⚠️ 不要在日志中输出明文 API Key

### 2. 权限验证

所有 API 操作都需要验证：
- ✅ 用户归属: `requireOwnedAuth()`
- ✅ 订单归属: `requireOwnedPosting()`
- ✅ 功能开关: `featureGate.assertPostingWriteEnabled()`

### 3. 输入验证

- ✅ 验证 authId, postingId 非空
- ✅ 验证取消原因非空
- ⚠️ 建议添加输入长度限制

---

## 故障排查

### 问题 1: API 调用失败

**症状**: 返回 401 错误

**排查步骤**:
1. 检查 Client-Id 和 Api-Key 是否正确
2. 检查授权是否过期
3. 查看 `t_ozon_api_log` 中的错误信息

### 问题 2: 订单状态不更新

**症状**: 取消订单后状态未变化

**排查步骤**:
1. 检查 API 响应是否成功
2. 检查数据库事务是否提交
3. 查看 `t_ozon_operation_audit` 中的操作记录

### 问题 3: 包裹同步无数据

**症状**: 同步包裹返回成功但无数据

**排查步骤**:
1. 检查 posting_number 是否正确
2. 检查订单是否已发货
3. 查看 API 响应 JSON 中的 result 数组

---

## 联系方式

如有 API 相关问题，请联系：

- **OZON 技术支持**: support@ozon.ru
- **API 文档**: https://docs.ozon.ru/api/seller/
- **开发者论坛**: https://forum.ozon.ru/

---

**文档版本**: 1.0
**创建日期**: 2026-06-25
**维护者**: Phase 5 实施团队
