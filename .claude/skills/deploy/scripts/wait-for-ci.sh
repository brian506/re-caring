#!/bin/bash
# PR의 CI checks가 모두 통과할 때까지 대기한다
# Usage: bash wait-for-ci.sh {PR번호}

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: bash wait-for-ci.sh {PR번호}" >&2
  exit 1
fi

PR_NUMBER="$1"
REPO="brian506/re-caring"

echo "=== Waiting for CI checks on PR #$PR_NUMBER ==="
gh pr checks "$PR_NUMBER" --repo "$REPO" --watch

STATUS=$(gh pr checks "$PR_NUMBER" --repo "$REPO" --json state --jq '.[].state' 2>/dev/null | sort -u)

if [ -z "$STATUS" ]; then
  echo "=== CI status 조회 실패 ===" >&2
  exit 1
fi

if echo "$STATUS" | grep -qiE "failure|error"; then
  echo "=== CI FAILED ==="

  # 실패한 run ID 조회
  RUN_ID=$(gh run list --repo "$REPO" --limit 5 --json databaseId,status,conclusion \
    --jq '.[] | select(.conclusion == "failure") | .databaseId' | head -1)

  if [ -n "$RUN_ID" ]; then
    echo "=== Failed run logs (run: $RUN_ID) ==="
    gh run view "$RUN_ID" --repo "$REPO" --log-failed
  fi

  exit 1
fi

echo "=== All CI checks passed ==="
