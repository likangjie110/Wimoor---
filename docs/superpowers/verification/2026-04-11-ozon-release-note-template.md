# Ozon Release Note Template

## 1. 使用方式

建议按以下顺序生成发布资产：

```bash
bash tools/ozon/local/90-full-check.sh
bash tools/ozon/local/95-release-handoff.sh
bash tools/ozon/local/96-verify-summary.sh
bash tools/ozon/local/97-release-note.sh
```

## 2. 模板结构

```text
# Ozon Release Note

Generated at:

## Release Snapshot
- Commit:
- Handoff:
- Verification Summary:

## Included Changes
- 

## Validation Coverage
- Backend verification:
- Frontend Playwright:
- Frontend build:
- Frontend audit:
- Local smoke:

## Release Notes
- User-facing changes:
- Operational changes:
- Feature gates involved:
- Risks / caveats:

## Rollback
- Rollback gates:
- Rollback owner:
- Trigger conditions:

## Links
- Gray rollout runbook:
- Release checklist:
- CI pipeline:
```

## 3. 建议填写原则

- `Included Changes` 只写本次发布范围内的 commit，不写历史背景。
- `Validation Coverage` 只引用已经通过的验证项。
- `Release Notes` 以运维和业务可读为主，不写实现细节。
- `Rollback` 必须写清 gate 和责任人。
