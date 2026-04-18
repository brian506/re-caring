#!/bin/bash
# develop 최신화 후 feature 브랜치를 생성한다
# Usage: bash setup-branch.sh {이슈번호}

ISSUE_NUMBER="$1"
BRANCH="feature/$ISSUE_NUMBER"

git checkout develop
git pull origin develop

if git show-ref --quiet "refs/heads/$BRANCH"; then
  echo "Branch $BRANCH already exists. Switching to it."
  git checkout "$BRANCH"
else
  git checkout -b "$BRANCH"
  echo "Created and switched to $BRANCH"
fi
