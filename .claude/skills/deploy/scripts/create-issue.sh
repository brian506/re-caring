#!/bin/bash
# GitHub 이슈를 생성하고 이슈 번호를 출력한다
# Usage: bash create-issue.sh "이슈 제목" "이슈 본문"

TITLE="$1"
BODY="$2"
REPO="brian506/re-caring"

ISSUE_URL=$(gh issue create \
  --repo "$REPO" \
  --title "$TITLE" \
  --body "$BODY" \
  --label "feature")

echo "Created issue: $ISSUE_URL"

# URL에서 이슈 번호 추출 (마지막 숫자 경로)
ISSUE_NUMBER=$(echo "$ISSUE_URL" | grep -o '[0-9]*$')
echo "Issue number: $ISSUE_NUMBER"
echo "$ISSUE_NUMBER"
