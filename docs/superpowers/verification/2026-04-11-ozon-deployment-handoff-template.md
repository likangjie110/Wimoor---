# Ozon Deployment Handoff Template

## 1. 使用方式

本模板用于 Ozon 发布交接。建议先运行：

```bash
bash tools/ozon/local/90-full-check.sh
```

然后执行：

```bash
bash tools/ozon/local/95-release-handoff.sh
```

脚本会在 `docs/superpowers/verification/` 下生成一份可编辑的 handoff 文档。

## 2. 模板内容

```text
# Ozon Release Handoff

Generated at:

## Release Meta
- Version:
- Environment:
- Branch:
- Head Commit:
- Working Tree:
- Gray Gates:
- Rollback Gates:

## Verification Assets
- Local verification report:
- Gray rollout runbook:
- Release checklist:
- CI pipeline:

## Recent Commits
- 

## Required Manual Fill
- Change summary:
- Validation owner:
- Release operator:
- Rollback owner:
- Remaining risks:
- Business observer:

## Final Gate Check
- [ ] Maven verification passed
- [ ] Frontend Playwright passed
- [ ] Frontend build passed
- [ ] Frontend audit passed
- [ ] Local smoke report reviewed
- [ ] Gray gate order confirmed
- [ ] Rollback gates prepared
```

## 3. 建议填写顺序

1. 先填 `Version / Environment / Gray Gates / Rollback Gates`
2. 再补 `Change summary` 和 `Remaining risks`
3. 最后由发布执行人确认 `Final Gate Check`

## 4. 归档要求

- handoff 文档与 local report 保留在 `docs/superpowers/verification/`
- 若走 CI 发布，建议把 workflow 链接也补进 handoff 文档
