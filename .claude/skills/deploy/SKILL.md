---
name: deploy
description: 전체 배포 파이프라인 자동화. 이슈 생성 → feature 브랜치 → 빌드/테스트 → 커밋 → PR → CI → 머지 → 배포 확인 → 브랜치 삭제. 사용자가 "커밋해줘", "배포해줘", "PR 올려줘", "deploy" 라고 하면 실행.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(curl *) Bash(bash *) Bash(chmod *) Read Grep Glob Edit
argument-hint: "[구현 내용 설명]"
---

# Deploy Pipeline

사용자가 전달한 구현 내용을 기반으로 아래 단계를 순서대로 실행한다.
각 단계가 실패하면 `references/error-handling.md`를 참고해 최대 3회 분석·수정 후 재시도한다.
3회 초과 시 사용자에게 보고하고 중단한다.

## Step 0: 이슈 생성

`assets/issue-template.md`를 참고해 이슈 제목과 본문을 작성한다.

```bash
bash scripts/create-issue.sh "{이슈 제목}" "{이슈 본문}"
```

반환된 이슈 번호(N)를 이후 모든 단계에 사용한다.

## Step 1: Feature 브랜치 생성

```bash
bash scripts/setup-branch.sh {N}
```

## Step 2: 빌드 & 테스트

```bash
bash scripts/build-and-test.sh
```

실패 시 `references/error-handling.md` → "빌드/테스트 실패" 섹션 참고.

## Step 3: 커밋 & 푸시

`references/commit-conventions.md`를 참고해 커밋 메시지를 작성한다.

```bash
bash scripts/commit-and-push.sh {N} "{커밋 설명}"
```

## Step 4: PR 생성

`assets/pr-template.md`를 읽어 PR 본문을 채운 후 실행한다.

```bash
bash scripts/create-pr.sh {N} "{PR 제목}"
```

반환된 PR 번호를 기록한다.

## Step 5: CI 통과 대기

```bash
bash scripts/wait-for-ci.sh {PR번호}
```

실패 시 `references/error-handling.md` → "CI 실패" 섹션 참고.

## Step 6: PR 머지 & 배포 대기

```bash
bash scripts/merge-and-deploy.sh {PR번호}
```

## Step 7: 헬스체크 & 브랜치 정리

```bash
bash scripts/health-check.sh
bash scripts/cleanup-branch.sh {N}
```

## 완료 보고

`assets/report-template.md` 형식으로 결과를 사용자에게 요약 보고한다.
