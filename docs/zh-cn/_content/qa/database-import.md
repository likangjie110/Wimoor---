# 数据库导入

## 导入顺序

1. 创建数据库。
2. 导入 `init-config/mysql/数据库结构`。
3. 导入 `init-config/mysql/数据`。
4. 确认 `db_quartz` 和 `db_admin.t_sys_quartz_task` 存在。
5. 启动服务并观察连接日志。

## 常见问题

- 字符集不一致导致中文异常。
- 基础数据未导入导致菜单、角色、任务为空。
- Quartz 表缺失导致定时任务启动失败。
- Seata 表缺失导致事务服务异常。

