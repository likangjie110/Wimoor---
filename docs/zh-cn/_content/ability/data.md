# 数据迁移

数据迁移能力由 `wimoor-modules/wimoor-data` 承载，网关路径为 `/mdata/**`。它用于迁移任务、迁移表信息、跨库或历史数据搬迁辅助。

## 功能范围

- 数据迁移任务。
- 迁移表信息。
- 跨库或历史数据搬迁辅助。
- 迁移前预检、执行后比对和异常记录。

## 模块边界

| 层级 | 位置 | 职责 |
| --- | --- | --- |
| 前端 API | `wimoorui/src/api/sys` 或数据迁移相关 API | 迁移任务配置和执行请求 |
| 后端服务 | `wimoor-modules/wimoor-data` | 数据迁移 Controller、Service、Mapper |
| 数据库 | `db_datamove` | 迁移任务、迁移表、迁移配置和执行记录 |
| 关联数据源 | 业务库 | 源库、目标库、历史库或临时库 |

## 迁移治理流程图

```mermaid
flowchart TB
  requirement["明确迁移需求"]
  source["确认源库/源表"]
  target["确认目标库/目标表"]
  mapping["字段映射和转换规则"]
  backup["备份和回滚方案"]
  precheck["预检：表结构、字段、数据量"]
  execute["执行迁移任务"]
  validate["校验记录数、关键字段、业务抽样"]
  publish["确认上线或交接"]
  rollback["异常回滚/重跑"]
  db["db_datamove 记录任务"]

  requirement --> source --> target --> mapping --> backup --> precheck --> execute --> validate --> publish
  execute --> db
  validate -- 不通过 --> rollback --> mapping
```

## 迁移执行时序图

```mermaid
sequenceDiagram
  autonumber
  actor Operator as 运维/开发人员
  participant UI as 数据迁移页面
  participant Gateway as wimoor-gateway
  participant Data as wimoor-data
  participant Source as 源数据库
  participant Target as 目标数据库
  participant LogDB as db_datamove

  Operator->>UI: 配置迁移任务和表映射
  UI->>Gateway: POST /mdata/api/** 任务配置
  Gateway->>Data: 保存任务
  Data->>LogDB: 写入任务、表、字段映射
  Operator->>UI: 执行预检
  UI->>Gateway: POST /mdata/api/** precheck
  Gateway->>Data: 触发预检
  Data->>Source: 检查源表结构和数据量
  Data->>Target: 检查目标表结构和约束
  Data->>LogDB: 写入预检结果
  Operator->>UI: 启动迁移
  Gateway->>Data: 执行迁移
  Data->>Source: 分批读取源数据
  Data->>Target: 写入目标数据
  Data->>LogDB: 记录进度、失败记录和耗时
  Data-->>UI: 返回执行结果
```

## 数据校验流程图

```mermaid
flowchart LR
  count["记录数对比"]
  key["主键/唯一键对比"]
  field["关键字段抽样"]
  relation["外键/业务关系检查"]
  report["迁移校验报告"]
  accept["确认验收"]

  count --> key --> field --> relation --> report --> accept
```

## 关键数据和接口

| 类型 | 说明 |
| --- | --- |
| 网关路径 | `/mdata/**` |
| API 前缀 | `/mdata/api/**` |
| 主要能力 | data move、table move、history data migration |
| 主要数据库 | `db_datamove` |
| 关联对象 | 源库、目标库、历史库、临时表 |

## 使用要求

- 生产数据迁移前必须确认备份、回滚策略、执行窗口和数据校验口径。
- 不要在未备份的生产库上直接执行迁移。
- 迁移任务应先在测试库或备份库演练。
- 涉及跨业务库迁移时，应由业务负责人确认字段映射和验收样本。

## 排错关注点

- 预检失败：检查源表、目标表、字段类型、索引和必填约束。
- 执行中断：检查批次大小、数据库连接、超时和失败记录。
- 记录数不一致：检查过滤条件、去重逻辑、增量时间范围和异常跳过策略。
- 业务校验不通过：检查字段转换、枚举映射、关联表顺序和历史数据质量。
