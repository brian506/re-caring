#!/bin/bash
# PR을 머지하고 deploy-dev.yml 워크플로우 완료까지 대기한다
# Usage: bash merge-and-deploy.sh {PR번호}

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: bash merge-and-deploy.sh {PR번호}" >&2
  exit 1
fi

PR_NUMBER="$1"
REPO="brian506/re-caring"

echo "=== Merging PR #$PR_NUMBER ==="
gh pr merge "$PR_NUMBER" \
  --repo "$REPO" \
  --merge \
  --delete-branch

echo "=== PR merged. Waiting for deploy-dev workflow ==="
sleep 10  # 워크플로우 트리거 대기

# merge commit SHA 기반으로 정확한 run 조회 (동시 배포 시 오인 방지)
MERGE_SHA=$(gh pr view "$PR_NUMBER" --repo "$REPO" --json mergeCommit --jq '.mergeCommit.oid')
echo "Merge SHA: $MERGE_SHA"

RUN_ID=$(gh run list \
  --repo "$REPO" \
  --workflow=deploy-dev.yml \
  --limit 20 \
  --json databaseId,headSha \
  --jq ".[] | select(.headSha == \"$MERGE_SHA\") | .databaseId" | head -1)

if [ -z "$RUN_ID" ]; then
  echo "=== deploy-dev run 조회 실패 (merge SHA: $MERGE_SHA) ===" >&2
  exit 1
fi

echo "Deploy run ID: $RUN_ID"
gh run watch "$RUN_ID" --repo "$REPO"

# 최종 상태 확인
CONCLUSION=$(gh run view "$RUN_ID" --repo "$REPO" --json conclusion --jq '.conclusion')
echo "Deploy result: $CONCLUSION"

if [ "$CONCLUSION" != "success" ]; then
  echo "=== DEPLOY FAILED. Fetching logs ===" >&2
  gh run view "$RUN_ID" --repo "$REPO" --log-failed
  exit 1
fi

echo "=== Deploy succeeded ==="
