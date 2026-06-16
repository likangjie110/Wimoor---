# 定时任务不执行

## 检查顺序

1. `wimoor-admin` 是否启动。
2. `db_quartz` 是否存在 Quartz 表。
3. `db_admin.t_sys_quartz_task` 是否有启用任务。
4. 任务 cron 是否正确。
5. 任务 path 指向的目标服务是否启动。
6. 目标接口是否被网关或安全配置拦截。

## 常见目标服务

种子任务主要调用 `wimoor-amazon`、`wimoor-amazon-adv`、`wimoor-erp`。

