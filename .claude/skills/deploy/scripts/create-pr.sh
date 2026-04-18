#!/bin/bash
# PR을 생성하고 PR 번호를 출력한다
# Usage: bash create-pr.sh {이슈번호} "PR 제목" "PR 본문"

set -euo pipefail

if [ "$#" -ne 3 ] || [ -z "${1:-}" ] || [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
  echo "Usage: bash create-pr.sh {이슈번호} \"PR 제목\" \"PR 본문\"" >&2
  exit 1
fi

ISSUE_NUMBER="$1"
TITLE="$2"
BODY="$3"
BRANCH="feature/$ISSUE_NUMBER"
REPO="brian506/re-caring"

PR_URL=$(gh pr create \
  --repo "$REPO" \
  --base develop \
  --head "$BRANCH" \
  --title "$TITLE" \
  --body "$BODY")

echo "Created PR: $PR_URL"

PR_NUMBER=$(echo "$PR_URL" | grep -o '[0-9]*$')
if [ -z "$PR_NUMBER" ]; then
  echo "PR 번호 추출 실패: $PR_URL" >&2
  exit 1
fi

echo "PR number: $PR_NUMBER"
echo "$PR_NUMBER"
