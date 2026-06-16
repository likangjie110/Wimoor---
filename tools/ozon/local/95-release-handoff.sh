#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
REPORT_DIR="${REPO_ROOT}/docs/superpowers/verification"

LOCAL_REPORT_FILE="${1:-}"
if [[ -z "${LOCAL_REPORT_FILE}" ]]; then
  LOCAL_REPORT_FILE="$(find "${REPORT_DIR}" -maxdepth 1 -type f -name 'ozon-local-report-*.md' | sort | tail -n 1)"
fi

if [[ -z "${LOCAL_REPORT_FILE}" || ! -f "${LOCAL_REPORT_FILE}" ]]; then
  echo "Missing local verification report for release handoff." >&2
  exit 1
fi

HANDOFF_FILE="${REPORT_DIR}/ozon-release-handoff-$(date +%Y%m%d-%H%M).md"
BRANCH_NAME="$(git -C "${REPO_ROOT}" rev-parse --abbrev-ref HEAD)"
HEAD_COMMIT="$(git -C "${REPO_ROOT}" rev-parse --short HEAD)"
WORKTREE_STATUS="$(git -C "${REPO_ROOT}" status --short)"
WORKTREE_SUMMARY="dirty"
WORKTREE_DETAIL_BLOCK=""
if [[ -z "${WORKTREE_STATUS}" ]]; then
  WORKTREE_SUMMARY="clean"
else
  WORKTREE_DETAIL_BLOCK=$'\n## Working Tree Details\n\n```text\n'"${WORKTREE_STATUS}"$'\n```\n'
fi

RECENT_COMMITS="$(git -C "${REPO_ROOT}" log -8 --pretty='- `%h` %s')"

cat > "${HANDOFF_FILE}" <<EOF
# Ozon Release Handoff

Generated at $(date '+%Y-%m-%d %H:%M:%S %z')

## Release Meta

- Version: ${RELEASE_VERSION:-TBD}
- Environment: ${RELEASE_ENV:-TBD}
- Branch: ${BRANCH_NAME}
- Head Commit: ${HEAD_COMMIT}
- Working Tree: ${WORKTREE_SUMMARY}
- Gray Gates: ${GRAY_GATES:-TBD}
- Rollback Gates: ${ROLLBACK_GATES:-TBD}

## Verification Assets

- Local verification report: ${LOCAL_REPORT_FILE}
- Gray rollout runbook: docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md
- Release checklist: docs/superpowers/verification/2026-04-11-ozon-release-checklist.md
- CI pipeline: docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md
- Handoff template: docs/superpowers/verification/2026-04-11-ozon-deployment-handoff-template.md

## Recent Commits

${RECENT_COMMITS}
${WORKTREE_DETAIL_BLOCK}

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
EOF

printf '%s\n' "${HANDOFF_FILE}"
