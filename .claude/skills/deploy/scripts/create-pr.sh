#!/bin/bash
# PR을 생성하고 PR 번호를 출력한다
# Usage: bash create-pr.sh {이슈번호} "PR 제목" "PR 본문"

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
echo "PR number: $PR_NUMBER"
echo "$PR_NUMBER"
