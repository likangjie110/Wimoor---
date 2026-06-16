# Ozon Verification Index Template

## 1. 目标

该索引页用于把 Ozon 发布相关的动态产物和静态文档统一到一页，降低交接时的检索成本。

## 2. 生成方式

```bash
bash tools/ozon/local/98-verify-index.sh
```

## 3. 预期内容

- 最新 local report
- 最新 release handoff
- 最新 verification summary
- 最新 release note
- 静态 runbook / checklist / template 链接

## 4. 推荐用法

- 发布前在交接群或工单里直接贴 `ozon-verification-index.md`
- 若某一项缺失，先补齐对应上游脚本产物再重新生成索引
