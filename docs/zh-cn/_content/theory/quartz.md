# 定时任务

定时任务由 Admin 服务的 Quartz JDBC store 承载，任务种子数据来自 `init-config/mysql/数据/db_admin/t_sys_quartz_task.sql`。

## 调用模型

```mermaid
sequenceDiagram
  participant Quartz as Admin Quartz
  participant Task as t_sys_quartz_task
  participant Service as 目标服务
  participant DB as 业务库
  Quartz->>Task: 读取任务定义
  Quartz->>Quartz: 注册 cron 触发器
  Quartz->>Service: 调用任务 path
  Service->>DB: 更新业务数据和任务状态
```

## 当前任务分布

种子任务主要目标服务为 `wimoor-amazon`、`wimoor-amazon-adv`、`wimoor-erp`。业务分组以 Amazon 报表、广告、商品、财务、订单为主。

