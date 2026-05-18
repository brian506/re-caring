---
name: hotfix
description: Emergency fix pipeline that commits directly to develop (no PR). For production-critical bugs only (outage, data corruption, security). Trigger on: '핫픽스', '긴급 수정', 'hotfix', '고쳐줘'.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Bash(sleep *) Bash(find *) Bash(grep *) Bash(curl *) Read Grep Glob Edit Write
argument-hint: "[수정 내용 설명]"
---

# Hotfix Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

> **주의**: 이 스킬은 `develop` 브랜치에 직접 커밋한다. feature 브랜치나 PR 없이 빠르게 프로덕션에 반영한다.
> 치명적 버그(서비스 다운, 데이터 손상 위험, 보안 취약점)에만 사용한다.

## Step 0: 상태 확인

```bash
git branch --show-current
git status
```

현재 브랜치가 `develop`이 아니면 사용자에게 알리고 중단한다.  
uncommitted 변경사항이 있으면 사용자에게 알리고 처리 방법을 확인한다.

## Step 1: 이슈 생성 (선택)

사용자가 이슈 번호를 제공하면 그 번호를 사용한다.  
제공하지 않으면 이슈를 생성할지 묻는다. 긴급 상황에서는 스킵 가능하다.

이슈를 생성하는 경우:
```bash
gh issue create --title "hotfix: {수정 내용}" --body "{상세 설명}" --label "bug"
```

이슈 번호(N)를 기록한다. 이슈 없이 진행하면 N=0으로 표시한다.

## Step 2: 코드 수정

사용자가 제공한 수정 내용을 분석해 코드를 수정한다.

수정 전 반드시 확인:
- `references/architecture.md` — 레이어 규칙 준수
- 기존 패턴을 벗어나지 않도록 유사 코드 먼저 확인

## Step 3: 빌드 & 테스트

```bash
./gradlew test 2>&1 | tail -60
```

실패 시 에러 로그를 분석해 수정한다. 반복 실패 시 사용자에게 보고하고 중단한다.

테스트 통과 후 빌드도 확인:
```bash
./gradlew build -x test 2>&1 | tail -30
```

## Step 4: 커밋 & 푸시

이슈 번호가 있는 경우:
```bash
git add {수정된 파일들}
git commit -m "hotfix[#{N}]: {수정 내용 설명}"
git push origin develop
```

이슈 번호가 없는 경우:
```bash
git add {수정된 파일들}
git commit -m "hotfix: {수정 내용 설명}"
git push origin develop
```

## Step 5: CI 확인

develop 브랜치 직접 푸시이므로 PR CI가 아닌 브랜치 CI를 확인한다. CI 트리거까지 잠시 대기 후 run ID를 확인한다.

```bash
gh run list --branch develop --limit 3
```

가장 최근 run ID를 확인해 결과를 기다린다:
```bash
gh run watch {run_id} --exit-status
```

CI 실패 시:
- 에러 로그를 분석한다: `gh run view {run_id} --log-failed`
- 원인을 수정하고 Step 3부터 반복한다
- 반복 실패 시 사용자에게 보고하고 중단한다

## Step 6: 배포 확인

CI 통과 후 GitHub Actions 배포 워크플로우가 자동 트리거된다. 배포 workflow 완료까지 대기 후 결과를 확인한다.

```bash
gh run list --branch develop --workflow deploy --limit 3
```

배포 완료 후 헬스체크:
```bash
curl -s --max-time 10 https://re-caring.duckdns.org/actuator/health
```

`{"status":"UP"}` 확인 시 완료.

## 완료 보고

```
핫픽스 완료
이슈: #{N} (없으면 "이슈 없이 진행")
커밋: {커밋 해시}
수정 내용: {설명}
배포: 성공
헬스체크: UP
```

배포 실패 또는 헬스체크 이상 시 즉시 사용자에게 알린다.

## Gotchas

- develop 브랜치가 아닌 상태에서 push하면 엉뚱한 브랜치에 커밋됨 — Step 0 브랜치 확인은 반드시 먼저
- uncommitted 변경사항이 있으면 `git add` 시 의도하지 않은 파일이 포함될 수 있음 — 파일 명시 필수
- CI가 아닌 deploy workflow를 별도로 확인해야 실제 배포 성공 여부를 알 수 있음 (CI 통과 ≠ 배포 성공)
- 헬스체크 `/actuator/health`가 UP이어도 ECS task가 rolling restart 중일 수 있음 — 배포 후 30초 이내 체크 결과는 신뢰하지 말 것
