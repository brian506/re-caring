#!/bin/bash
# PR을 생성하고 PR 번호를 출력한다
# Usage: bash create-pr.sh {이슈번호} "이슈 제목" "PR 본문"
#   제목은 이슈 제목만 넘긴다. "{type}[#{N}]: " 접두사는 이 스크립트가 붙인다.

set -euo pipefail

if [ "$#" -ne 3 ] || [ -z "${1:-}" ] || [ -z "${2:-}" ] || [ -z "${3:-}" ]; then
  echo "Usage: bash create-pr.sh {이슈번호} \"PR 제목\" \"PR 본문\"" >&2
  exit 1
fi

ISSUE_NUMBER="$1"
TITLE="$2"
BODY="$3"
# Use the actual checked-out branch (any {type}/{N} form), not a reconstructed feature/{N}
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
TYPE="${BRANCH%%/*}"
REPO="brian506/re-caring"
ASSIGNEE=$(gh api user --jq '.login')

# 제목 접두사는 호출자에게 맡기지 않는다 (매번 이슈 번호가 누락되던 원인).
# 이미 붙어 있으면 중복되지 않도록 벗겨낸 뒤 다시 조립한다.
TITLE="$(printf '%s' "$TITLE" | sed -E 's/^[a-z]+\[#[0-9]+\][[:space:]]*:[[:space:]]*//')"
TITLE="$(printf '%s' "$TITLE" | sed -E 's/^\[[^]]+\][[:space:]]*:?[[:space:]]*//')"
TITLE="${TYPE}[#${ISSUE_NUMBER}]: ${TITLE}"
echo "PR title: $TITLE"

PR_URL=$(gh pr create \
  --repo "$REPO" \
  --base develop \
  --head "$BRANCH" \
  --title "$TITLE" \
  --body "$BODY" \
  --assignee "$ASSIGNEE")

echo "Created PR: $PR_URL"

# Best-effort label by branch type (label may not exist in the repo)
gh pr edit "$PR_URL" --repo "$REPO" --add-label "$TYPE" >/dev/null 2>&1 || true

PR_NUMBER=$(echo "$PR_URL" | grep -o '[0-9]*$')
if [ -z "$PR_NUMBER" ]; then
  echo "PR 번호 추출 실패: $PR_URL" >&2
  exit 1
fi

echo "PR number: $PR_NUMBER"
echo "$PR_NUMBER"
