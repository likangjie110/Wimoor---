#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
REPORT_DIR="${REPO_ROOT}/docs/superpowers/verification"
INDEX_FILE="${REPORT_DIR}/ozon-verification-index.md"

latest_file() {
  local pattern="$1"
  find "${REPORT_DIR}" -maxdepth 1 -type f -name "${pattern}" | sort | tail -n 1
}

latest_local_report="$(latest_file 'ozon-local-report-*.md')"
latest_handoff="$(latest_file 'ozon-release-handoff-*.md')"
latest_summary="$(latest_file 'ozon-verify-summary-*.md')"
latest_release_note="$(latest_file 'ozon-release-note-*.md')"

cat > "${INDEX_FILE}" <<EOF
# Ozon Verification Index

Generated at $(date '+%Y-%m-%d %H:%M:%S %z')

## Latest Generated Assets

- Local report: ${latest_local_report:-TBD}
- Release handoff: ${latest_handoff:-TBD}
- Verification summary: ${latest_summary:-TBD}
- Release note: ${latest_release_note:-TBD}

## Static References

- Gray rollout runbook: docs/superpowers/verification/2026-04-11-ozon-gray-rollout-runbook.md
- Release checklist: docs/superpowers/verification/2026-04-11-ozon-release-checklist.md
- CI pipeline: docs/superpowers/verification/2026-04-11-ozon-ci-pipeline.md
- Deployment handoff template: docs/superpowers/verification/2026-04-11-ozon-deployment-handoff-template.md
- Release note template: docs/superpowers/verification/2026-04-11-ozon-release-note-template.md

## Recommended Generation Order

1. bash tools/ozon/local/90-full-check.sh
2. bash tools/ozon/local/95-release-handoff.sh
3. bash tools/ozon/local/96-verify-summary.sh
4. bash tools/ozon/local/97-release-note.sh
5. bash tools/ozon/local/98-verify-index.sh
EOF

printf '%s\n' "${INDEX_FILE}"
