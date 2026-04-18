#!/bin/bash
# 로컬 feature 브랜치를 삭제하고 develop을 최신화한다
# Usage: bash cleanup-branch.sh {이슈번호}

ISSUE_NUMBER="$1"
BRANCH="feature/$ISSUE_NUMBER"

git checkout develop
git pull origin develop
git branch -d "$BRANCH" 2>/dev/null && echo "Deleted local branch: $BRANCH" \
  || echo "Local branch $BRANCH not found or already deleted"

echo "=== Cleanup done. Now on develop ==="
