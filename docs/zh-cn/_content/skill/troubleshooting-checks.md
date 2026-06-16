# 排错检查

## 通用顺序

1. 看启动日志中第一个异常。
2. 确认 Nacos 配置是否读取成功。
3. 确认数据库连接、账号、库名是否正确。
4. 确认 Redis、Seata 是否按当前服务需要启动。
5. 确认网关路由和前端代理路径是否一致。
6. 确认服务端口没有冲突。

## 快速定位

| 现象 | 优先检查 |
| --- | --- |
| 服务启动失败 | bootstrap、Nacos、数据库、端口 |
| 前端接口 404 | Vite proxy、gateway route、Controller path |
| 登录后无菜单 | Admin 用户角色菜单数据、权限接口、前端 dynamic route |
| 定时任务不执行 | `t_sys_quartz_task`、`db_quartz`、Admin Quartz 配置 |
| 跨服务调用失败 | Nacos 服务发现、Feign target、目标服务是否启动 |

