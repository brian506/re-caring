#!/bin/bash
# 변경사항을 커밋하고 원격 브랜치에 푸시한다
# Usage: bash commit-and-push.sh {이슈번호} "커밋 설명" [type]
#   type: feature(기본값) | fix | refactor | style | docs | test | hotfix | chore

set -euo pipefail

if [ -z "${1:-}" ] || [ -z "${2:-}" ]; then
  echo "Usage: bash commit-and-push.sh {이슈번호} \"커밋 설명\" [type]" >&2
  exit 1
fi

ISSUE_NUMBER="$1"
DESCRIPTION="$2"
TYPE="${3:-feature}"

case "$TYPE" in
  feature|fix|refactor|style|docs|test|hotfix|chore) ;;
  *)
    echo "Invalid type: $TYPE. Allowed: feature, fix, refactor, style, docs, test, hotfix, chore" >&2
    exit 1
    ;;
esac

BRANCH="feature/$ISSUE_NUMBER"
COMMIT_MSG="$TYPE[#$ISSUE_NUMBER]: $DESCRIPTION"

git add -A
echo "=== Staged files ==="
git status --short

git commit -m "$COMMIT_MSG"
git push origin "$BRANCH"

echo "=== Committed: $COMMIT_MSG ==="
