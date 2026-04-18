#!/bin/bash
# 변경사항을 커밋하고 원격 브랜치에 푸시한다
# Usage: bash commit-and-push.sh {이슈번호} "커밋 설명"

ISSUE_NUMBER="$1"
DESCRIPTION="$2"
BRANCH="feature/$ISSUE_NUMBER"
COMMIT_MSG="feature[#$ISSUE_NUMBER]: $DESCRIPTION"

git add -A
echo "=== Staged files ==="
git status --short

git commit -m "$COMMIT_MSG"
git push origin "$BRANCH"

echo "=== Committed: $COMMIT_MSG ==="
