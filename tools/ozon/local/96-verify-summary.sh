#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
REPORT_DIR="${REPO_ROOT}/docs/superpowers/verification"

LOCAL_REPORT_FILE="${1:-}"
HANDOFF_FILE="${2:-}"

if [[ -z "${LOCAL_REPORT_FILE}" ]]; then
  LOCAL_REPORT_FILE="$(find "${REPORT_DIR}" -maxdepth 1 -type f -name 'ozon-local-report-*.md' | sort | tail -n 1)"
fi

if [[ -z "${HANDOFF_FILE}" ]]; then
  HANDOFF_FILE="$(find "${REPORT_DIR}" -maxdepth 1 -type f -name 'ozon-release-handoff-*.md' | sort | tail -n 1)"
fi

if [[ -z "${LOCAL_REPORT_FILE}" || ! -f "${LOCAL_REPORT_FILE}" ]]; then
  echo "Missing local verification report for summary." >&2
  exit 1
fi

if [[ -z "${HANDOFF_FILE}" || ! -f "${HANDOFF_FILE}" ]]; then
  echo "Missing release handoff document for summary." >&2
  exit 1
fi

SUMMARY_FILE="${REPORT_DIR}/ozon-verify-summary-$(date +%Y%m%d-%H%M).md"
HEAD_COMMIT="$(git -C "${REPO_ROOT}" rev-parse --short HEAD)"
BRANCH_NAME="$(git -C "${REPO_ROOT}" rev-parse --abbrev-ref HEAD)"
WORKTREE_STATUS="$(git -C "${REPO_ROOT}" status --short)"
WORKTREE_SUMMARY="dirty"
if [[ -z "${WORKTREE_STATUS}" ]]; then
  WORKTREE_SUMMARY="clean"
fi

collect_sections() {
  local file="$1"
  grep -E '^## ' "${file}" || true
}

REPORT_SECTIONS="$(collect_sections "${LOCAL_REPORT_FILE}")"
HANDOFF_SECTIONS="$(collect_sections "${HANDOFF_FILE}")"
if [[ -z "${REPORT_SECTIONS}" ]]; then
  REPORT_SECTIONS="(no sections found)"
fi
if [[ -z "${HANDOFF_SECTIONS}" ]]; then
  HANDOFF_SECTIONS="(no sections found)"
fi
RECENT_COMMITS="$(git -C "${REPO_ROOT}" log -5 --pretty='- `%h` %s')"

cat > "${SUMMARY_FILE}" <<EOF
# Ozon Verification Summary

Generated at $(date '+%Y-%m-%d %H:%M:%S %z')

## Snapshot

- Branch: ${BRANCH_NAME}
- Head Commit: ${HEAD_COMMIT}
- Working Tree: ${WORKTREE_SUMMARY}
- Local Report: ${LOCAL_REPORT_FILE}
- Release Handoff: ${HANDOFF_FILE}

## Recent Commits

${RECENT_COMMITS}

## Local Report Sections

~~~text
${REPORT_SECTIONS}
~~~

## Release Handoff Sections

~~~text
${HANDOFF_SECTIONS}
~~~

## Review Checklist

- [ ] Local report exists and is readable
- [ ] Release handoff exists and contains release meta
- [ ] Verification chain has been reviewed by release owner
- [ ] Gray gates and rollback gates are explicitly filled
- [ ] Remaining risks are documented before release
EOF

printf '%s\n' "${SUMMARY_FILE}"
