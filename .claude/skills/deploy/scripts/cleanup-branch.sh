#!/bin/bash
# 로컬 feature 브랜치를 삭제하고 develop을 최신화한다
# Usage: bash cleanup-branch.sh {이슈번호}

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: bash cleanup-branch.sh {이슈번호}" >&2
  exit 1
fi

ISSUE_NUMBER="$1"
BRANCH="feature/$ISSUE_NUMBER"

if ! git checkout develop; then
  echo "WARN: develop 브랜치 checkout 실패" >&2
  exit 1
fi

if ! git pull origin develop; then
  echo "WARN: develop 최신화 실패" >&2
  exit 1
fi

git branch -d "$BRANCH" 2>/dev/null && echo "Deleted local branch: $BRANCH" \
  || echo "Local branch $BRANCH not found or already deleted"

echo "=== Cleanup done. Now on develop ==="
