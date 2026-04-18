#!/bin/bash
# PR을 머지하고 deploy-dev.yml 워크플로우 완료까지 대기한다
# Usage: bash merge-and-deploy.sh {PR번호}

PR_NUMBER="$1"
REPO="brian506/re-caring"

echo "=== Merging PR #$PR_NUMBER ==="
gh pr merge "$PR_NUMBER" \
  --repo "$REPO" \
  --merge \
  --delete-branch

echo "=== PR merged. Waiting for deploy-dev workflow ==="
sleep 10  # 워크플로우 트리거 대기

# 최신 deploy-dev run 조회
RUN_ID=$(gh run list \
  --repo "$REPO" \
  --workflow=deploy-dev.yml \
  --limit 1 \
  --json databaseId \
  --jq '.[0].databaseId')

echo "Deploy run ID: $RUN_ID"
gh run watch "$RUN_ID" --repo "$REPO"

# 최종 상태 확인
CONCLUSION=$(gh run view "$RUN_ID" --repo "$REPO" --json conclusion --jq '.conclusion')
echo "Deploy result: $CONCLUSION"

if [ "$CONCLUSION" != "success" ]; then
  echo "=== DEPLOY FAILED. Fetching logs ==="
  gh run view "$RUN_ID" --repo "$REPO" --log-failed
  exit 1
fi

echo "=== Deploy succeeded ==="
