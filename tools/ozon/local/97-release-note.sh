#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
REPORT_DIR="${REPO_ROOT}/docs/superpowers/verification"

HANDOFF_FILE="${1:-}"
SUMMARY_FILE="${2:-}"

if [[ -z "${HANDOFF_FILE}" ]]; then
  HANDOFF_FILE="$(find "${REPORT_DIR}" -maxdepth 1 -type f -name 'ozon-release-handoff-*.md' | sort | tail -n 1)"
fi

if [[ -z "${SUMMARY_FILE}" ]]; then
  SUMMARY_FILE="$(find "${REPORT_DIR}" -maxdepth 1 -type f -name 'ozon-verify-summary-*.md' | sort | tail -n 1)"
fi

if [[ -z "${HANDOFF_FILE}" || ! -f "${HANDOFF_FILE}" ]]; then
  echo "Missing release handoff for release note." >&2
  exit 1
fi

if [[ -z "${SUMMARY_FILE}" || ! -f "${SUMMARY_FILE}" ]]; then
  echo "Missing verification summary for release note." >&2
  exit 1
fi

RELEASE_NOTE_FILE="${REPORT_DIR}/ozon-release-note-$(date +%Y%m%d-%H%M).md"
HEAD_COMMIT="$(git -C "${REPO_ROOT}" rev-parse --short HEAD)"
RECENT_COMMITS="$(git -C "${REPO_ROOT}" log -5 --pretty='- `%h` %s')"

cat > "${RELEASE_NOTE_FILE}" <<EOF
# Ozon Release Note

Generated at $(date '+%Y-%m-%d %H:%M:%S %z')

## Release Snapshot

- Commit: ${HEAD_COMMIT}
- Handoff: ${HANDOFF_FILE}
- Verification Summary: ${SUMMARY_FILE}

## Included Changes

${RECENT_COMMITS}

## Validation Coverage

- Backend verification: see local report / CI backend artifact
- Frontend Playwright: see verification summary / CI frontend artifact
- Frontend build: required
- Frontend audit: required
- Local smoke: required before gray release

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

- Gray rollout runbook: docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md
- Release checklist: docs/superpowers/verification/2026-04-11-ozon-release-checklist.md
- CI pipeline: docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md
EOF

printf '%s\n' "${RELEASE_NOTE_FILE}"
