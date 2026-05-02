---
name: deploy
description: feature 브랜치의 변경사항을 빌드/테스트 → 커밋 → PR → CI 대기 → 머지 → 배포 확인 → 브랜치 삭제까지 자동화한다.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Bash(sleep *) Read Glob
---

# Deploy Agent

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

현재 feature 브랜치의 변경사항을 배포 파이프라인 끝까지 자동 처리한다.
스크립트는 모두 `.claude/skills/deploy/scripts/` 에 있다.

## 사전 확인

현재 브랜치와 이슈 번호를 파악한다.

```bash
git branch --show-current
```

브랜치명이 `feature/{N}` 형태여야 한다. N이 이슈 번호다.

## Step 1: 빌드 & 테스트

```bash
bash .claude/skills/deploy/scripts/build-and-test.sh
```

실패 시 `references/error-handling.md`를 참고해 원인을 분석하고 코드를 수정한다.
3회 반복 후에도 실패하면 사용자에게 보고하고 중단한다.

## Step 2: 커밋 & 푸시

커밋 설명은 prompt에서 전달받은 내용을 사용한다. 없으면 변경된 파일을 보고 적절히 작성한다.
`references/commit-conventions.md`를 참고해 type을 결정한다.

```bash
bash .claude/skills/deploy/scripts/commit-and-push.sh {N} "{커밋 설명}" {type}
```

## Step 3: PR 생성

PR 제목은 반드시 아래 형식을 따른다 (commit-conventions.md와 동일):

```
{type}[#{N}]: {설명}
```

예시: `feat[#42]: 회원 탈퇴 API 구현`, `fix[#55]: Redis config 경로 수정`

PR 본문은 `assets/pr-template.md`를 참고해 작성한다.

```bash
bash .claude/skills/deploy/scripts/create-pr.sh {N} "{PR 제목}" "{PR 본문}"
```

출력에서 PR 번호를 추출해 이후 단계에 사용한다.

## Step 4: CI 대기

```bash
bash .claude/skills/deploy/scripts/wait-for-ci.sh {PR번호}
```

CI 실패 시 `references/error-handling.md`의 "CI 실패" 섹션을 참고한다.
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
PR: #{PR번호}
배포: 성공
브랜치: feature/{N} 삭제 완료
현재 브랜치: develop
```
