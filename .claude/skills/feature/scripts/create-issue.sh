#!/bin/bash
# GitHub 이슈를 이슈 템플릿 형식에 맞게 생성한다
#
# Usage:
#   bash create-issue.sh feature  "제목 (prefix 제외)" "설명" "작업항목 (마크다운 체크리스트)"
#   bash create-issue.sh bug      "제목" "설명" "기대 동작" ["첨부 자료"]
#   bash create-issue.sh refactor "제목" "설명" "작업항목 (마크다운 체크리스트)"
#
# 이슈 유형별 템플릿 (.github/ISSUE_TEMPLATE/{type}.yml) 에서 제목 prefix를 자동 추출한다.

set -euo pipefail

ISSUE_TYPE="${1:-feature}"
TITLE_BODY="${2:-}"
DESCRIPTION="${3:-}"
ARG4="${4:-}"
ARG5="${5:-}"

if [ -z "$TITLE_BODY" ] || [ -z "$DESCRIPTION" ]; then
  echo "Usage: bash create-issue.sh <feature|bug|refactor> \"제목\" \"설명\" [\"작업항목\"]" >&2
  exit 1
fi

TEMPLATE_FILE=".github/ISSUE_TEMPLATE/${ISSUE_TYPE}.yml"
if [ ! -f "$TEMPLATE_FILE" ]; then
  echo "Unknown issue type '${ISSUE_TYPE}'. Template not found: $TEMPLATE_FILE" >&2
  exit 1
fi

# 제목 prefix 추출 — title: "[Feature]: " → [Feature]:
TITLE_PREFIX=$(grep '^title:' "$TEMPLATE_FILE" | sed 's/^title: //;s/"//g')
FULL_TITLE="${TITLE_PREFIX}${TITLE_BODY}"

# 이슈 유형별 본문 구성
case "$ISSUE_TYPE" in
  feature|refactor)
    TASKS="${ARG4:-"- [ ] TODO"}"
    BODY="## 📄 설명
${DESCRIPTION}

## ✅ 작업할 내용
${TASKS}"
    ;;
  bug)
    BODY="## 📄 설명
${DESCRIPTION}

## ✅ 기대 동작
${ARG4:-}

## 🖼️ 첨부 자료
${ARG5:-}"
    ;;
  *)
    BODY="$DESCRIPTION"
    ;;
esac

REPO="brian506/re-caring"
ASSIGNEE=$(gh api user --jq '.login')

ISSUE_URL=$(gh issue create \
  --repo "$REPO" \
  --title "$FULL_TITLE" \
  --body "$BODY" \
  --label "$ISSUE_TYPE" \
  --assignee "$ASSIGNEE")

echo "Created issue: $ISSUE_URL"

ISSUE_NUMBER=$(echo "$ISSUE_URL" | grep -o '[0-9]*$')
if [ -z "$ISSUE_NUMBER" ]; then
  echo "Failed to parse issue number from URL: $ISSUE_URL" >&2
  exit 1
fi

echo "Issue number: $ISSUE_NUMBER"
echo "$ISSUE_NUMBER"
