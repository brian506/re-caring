---
name: deploy
description: feature 브랜치의 변경사항을 빌드/테스트 → 커밋 → PR → CI 대기 → 머지 → 배포 확인 → 브랜치 삭제까지 자동화한다. 사용자가 "배포해줘" 라는 명령을 전달하면 수행한다.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Bash(sleep *) Read Glob
---

# Deploy Agent

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

현재 feature 브랜치의 변경사항을 배포 파이프라인 끝까지 자동 처리한다.
스크립트는 모두 `.claude/skills/deploy/scripts/` 에 있다.

## Step 0: 이슈 확인 및 생성

GitHub에 이슈 #{N}이 실제로 존재하는지 확인한다.

```bash
gh issue view {N} --json number,title,state 2>&1
```

이슈가 존재하면 제목을 기록하고 Step 1로 넘어간다.

이슈가 존재하지 않으면 (오류 출력 또는 "no issue" 응답) **변경 파일을 분석해 이슈를 자동 생성**한다.

### 자동 이슈 생성 절차

**① 변경 파일 목록 확인**

```bash
git diff --name-only develop...HEAD
```

**② 파일 구성으로 제목·본문·label 추론**

변경된 파일의 경로·이름·내용을 읽어 아래를 결정한다:

- **이슈 제목**: `{type}: {도메인} {동작} 기능 구현` 형식  
  예) `feature: 보호자 위치 설정 API 구현`, `fix: 회원 탈퇴 시 토큰 미삭제 버그 수정`
- **이슈 본문**: 변경된 계층(Controller/Service/Repository 등)과 주요 파일 목록을 나열
- **label** 선택 기준:
  - Controller·Service·Repository 추가 → `feature`
  - 버그 수정 패턴(fix, bugfix 포함) → `bug`
  - 리팩터링만 → `refactor`
  - 빌드·설정 파일만 → `chore`
  - 인프라·배포 설정 → `infra`

**③ 이슈 생성**

```bash
bash .claude/skills/deploy/scripts/create-issue.sh "{이슈 제목}" "{이슈 본문}" "{label}"
```

생성된 이슈 번호가 브랜치의 N과 일치하지 않으면 사용자에게 알리고 진행 여부를 확인한다.

## Step 1: 빌드 & 테스트

```bash
bash .claude/skills/deploy/scripts/build-and-test.sh
```

실패 시 `.claude/skills/deploy/references/error-handling.md` 파일을 Read 도구로 읽은 뒤 원인을 분석하고 코드를 수정한다.
3회 반복 후에도 실패하면 사용자에게 보고하고 중단한다.

## Step 2: 커밋 & 푸시

먼저 `.claude/skills/deploy/references/commit-conventions.md` 파일을 Read 도구로 읽는다.

커밋 설명은 prompt에서 전달받은 내용을 사용한다. 없으면 변경된 파일을 보고 적절히 작성한다.
읽은 컨벤션에 따라 type을 결정한다.

```bash
bash .claude/skills/deploy/scripts/commit-and-push.sh {N} "{커밋 설명}" {type}
```

## Step 3: PR 생성

먼저 `.claude/skills/deploy/assets/pr-template.md` 파일을 Read 도구로 읽는다.

PR 제목 형식 (읽은 commit-conventions.md 기준):

```
{type}[#{N}]: {설명}
```

예시: `feat[#42]: 회원 탈퇴 API 구현`, `fix[#55]: Redis config 경로 수정`

읽은 템플릿을 바탕으로 PR 본문을 작성한다. `closes #{N}` 을 반드시 포함한다.

```bash
bash .claude/skills/deploy/scripts/create-pr.sh {N} "{PR 제목}" "{PR 본문}"
```

출력에서 PR 번호를 추출해 이후 단계에 사용한다.

## Step 4: CI 대기

```bash
bash .claude/skills/deploy/scripts/wait-for-ci.sh {PR번호}
```

CI 실패 시 `.claude/skills/deploy/references/error-handling.md` 파일을 Read 도구로 읽고 "CI 실패" 섹션을 참고한다.
원인 수정 후 `commit-and-push.sh`로 재푸시하면 CI가 자동 재트리거된다.
수정 커밋은 type을 `fix`로 지정한다.

## Step 5: 머지 & 배포

```bash
bash .claude/skills/deploy/scripts/merge-and-deploy.sh {PR번호}
```

배포 실패 시 인프라 레벨 이슈가 많으므로 원인을 분석해 사용자에게 보고하고 중단한다.

## Step 6: 브랜치 정리

```bash
bash .claude/skills/deploy/scripts/cleanup-branch.sh {N}
```

## 완료 보고

```
이슈: #{N}
PR: #{PR번호}
배포: 성공
브랜치: feature/{N} 삭제 완료
현재 브랜치: develop
```
