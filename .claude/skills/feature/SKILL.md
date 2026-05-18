---
name: feature
description: Implements new features following project architecture rules (issue → branch → code). Does NOT write tests — use /test-write for that. Trigger on: '~API 만들어줘', '~기능 구현해줘', '~추가해줘', '~만들어줘'.
allowed-tools: Agent Bash(git branch *) Bash(gh issue *) Bash(gh issue list *) Bash(bash .claude/skills/feature/scripts/*)
argument-hint: "[구현할 기능 설명]"
---

# Feature Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

사용자가 전달한 기능 설명을 기반으로 아래 단계를 순서대로 실행한다.

## Step 0: 이슈 확인 및 생성

기존 이슈 중 관련된 것이 있는지 확인 후 재사용 또는 신규 생성한다.

```bash
gh issue list --state open --limit 50
```

관련 이슈가 없으면 생성한다:

```bash
bash .claude/skills/feature/scripts/create-issue.sh "<feature|bug|refactor>" "제목 (prefix 제외)" "설명" "작업항목"
```

이슈 유형별 `.github/ISSUE_TEMPLATE/{type}.yml` 형식이 자동 적용된다:
- `feature`  → `[Feature]: 제목`  / 📄 설명 + ✅ 작업할 내용
- `bug`      → `[Bug]: 제목`      / 📄 설명 + ✅ 기대 동작 + 🖼️ 첨부 자료
- `refactor` → `[Refactor]: 제목` / 📄 설명 + ✅ 작업할 내용

반환된 이슈 번호(N)를 이후 모든 단계에 사용한다.

## Step 1: Feature 브랜치 생성

```bash
bash .claude/skills/feature/scripts/setup-branch.sh {N}
```

## Step 2: 코드 구현 위임

`implement` subagent를 spawn하여 실제 코드 구현을 위임한다.

Agent tool로 `subagent_type="implement"`으로 spawn한다. prompt에 반드시 아래를 포함한다:
- 이슈 번호: #{N}
- 이슈 제목 및 설명
- 구현할 기능 상세 (사용자가 전달한 내용 전체)

subagent의 완료 보고를 기다린 뒤 결과를 사용자에게 전달한다.

## 완료 보고

```
이슈: #{N}
브랜치: feature/{N}
다음 단계: 테스트·빌드 준비되면 "테스트 작성해줘" 또는 /deploy 로 배포 진행
```
