#!/bin/bash
# develop 최신화 후 feature 브랜치를 생성한다
# Usage: bash setup-branch.sh {이슈번호}

set -euo pipefail

if [ -z "${1:-}" ]; then
  echo "Usage: bash setup-branch.sh {이슈번호}" >&2
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

if git show-ref --quiet "refs/heads/$BRANCH"; then
  echo "Branch $BRANCH already exists locally. Switching to it."
  git checkout "$BRANCH"
elif git ls-remote --exit-code --heads origin "$BRANCH" > /dev/null 2>&1; then
  echo "Remote branch $BRANCH exists. Tracking it."
  git checkout -b "$BRANCH" --track "origin/$BRANCH"
else
  git checkout -b "$BRANCH"
  echo "Created and switched to $BRANCH"
fi
