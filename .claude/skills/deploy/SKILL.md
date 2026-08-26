---
name: deploy
description: Automates branch deployment pipeline (commit → PR → CI → merge → deploy). Runs on issue-linked branches of the form {type}/{N} (feature, refactor, fix, chore, hotfix, ...). Trigger on: '배포해줘', '커밋해줘', 'PR 올려줘', 'deploy', 'merge해줘'.
allowed-tools: Agent Bash(git branch *) Bash(git diff *) Bash(gh issue view *)
argument-hint: "[커밋 설명]"
---

# Deploy

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

## 사전 브랜치 확인 (필수 — 실패 시 즉시 중단)

subagent를 spawn하기 전에 반드시 현재 브랜치를 확인한다.

```bash
git branch --show-current
```

출력값이 `{type}/{N}...` 형태(`type`은 `feature`·`refactor`·`fix`·`chore`·`hotfix` 등 소문자 접두사, `N`은 이슈 번호. 예: `feature/42`, `refactor/132-remove-soft-delete`)가 **아니라면** 즉시 중단하고 아래 메시지를 사용자에게 출력한 뒤 파이프라인을 종료한다. **어떠한 경우에도 다음 단계로 넘어가지 않는다.**

판정 정규식: `^[a-z]+/[0-9]+`. 브랜치 번호 `N`은 첫 슬래시 뒤 숫자열에서 추출한다(예: `refactor/132-remove-soft-delete` → `132`).

```
[배포 중단] 현재 브랜치가 {type}/{N} 형태가 아닙니다. (현재: {브랜치명})
/deploy는 이슈 번호가 붙은 브랜치({type}/{N})에서만 사용할 수 있습니다.

- 새 기능 개발: /feature-dev 로 이슈·브랜치를 먼저 생성하세요.
- 긴급 수정: /hotfix 를 사용하세요.
```

브랜치가 `{type}/{N}` 형태인 경우에만 아래를 계속 진행한다.

---

## 이슈 존재 확인 (필수 — 없으면 즉시 중단)

브랜치 번호 N을 추출해 이슈가 실제로 존재하는지 확인한다.

```bash
gh issue view {N} --json number,title,state 2>&1
```

이슈가 존재하지 않으면 즉시 중단하고 아래 메시지를 출력한다. subagent를 spawn하지 않는다.

```
[배포 중단] 이슈 #{N}이 GitHub에 존재하지 않습니다.
/deploy는 이미 이슈가 생성된 브랜치({type}/{N})에서만 실행할 수 있습니다.
/feature-dev로 이슈와 브랜치를 먼저 생성하세요.
```

이슈가 존재하면 아래를 계속 진행한다.

---

## 1단계: 빌드·테스트·커밋 (`deploy-runner` phase=prepare)

Agent tool로 subagent_type="deploy-runner"를 spawn한다.
prompt에 **`phase=prepare`** 와 커밋 설명을 포함한다.
에이전트가 빌드·테스트·커밋·푸시까지 하고 돌아온다.

---

## 2단계: 리뷰 게이트 — PR 생성 전 필수

**여기서 실행하는 이유:** 리뷰 에이전트들은 `git diff develop...HEAD`를 본다.
이건 커밋된 것만 잡으므로 1단계가 끝난 뒤여야 한다.
그리고 서브에이전트가 서브에이전트를 spawn할 수 없으므로, deploy 에이전트 안이 아니라
메인 컨텍스트인 여기서 호출해야 한다.

변경 범위를 먼저 확인한다. **해당 파일이 없는 섹션은 건너뛴다.**

```bash
git diff develop...HEAD --name-only -- 'src/main/**'
git diff develop...HEAD --name-only -- 'src/test/**'
```

섹션은 서로 독립이므로 **한 번에 병렬 spawn한다.** 각 에이전트가 별도 컨텍스트를 쓰므로
관점별로 나누는 목적(다른 관점에 오염되지 않은 판단)은 병렬로도 그대로 달성된다.

### 구현 — `src/main/**` 변경이 있을 때

| 섹션 | 에이전트 | 차단 기준 |
|------|----------|----------|
| 아키텍처 | `review` | 🔴 Critical |
| 보안 | `security-auditor` | 🔴 Critical, 🟠 High → 차단 / 🟡 Medium → 사용자 확인 |

### 테스트 — `src/test/**` 변경이 있을 때

| 섹션 | 에이전트 | 차단 기준 |
|------|----------|----------|
| 안티패턴·기댓값 출처 | `test-review` | `판정: 수정 필요` |

> 섹션은 **한 번에 하나씩** 늘린다. 새 관점(에러 처리, 성능 등)이 필요하면 전용 에이전트를
> 먼저 만들고 표에 추가한다. 에이전트 없이 관점 이름만 늘리면 게이트가 형식만 남는다.
> 팀이 무시하기 시작하는 섹션은 차단에서 경고로 내리거나 제거한다.

### 판정 처리

- **모든 지적에 근거 한 줄이 붙어 있어야 한다.** 근거 없는 지적은 채택하지 않는다
- 차단 기준에 걸리면 **3단계로 넘어가지 않는다.** 수정 → 커밋 → 이 단계를 다시 실행한다
- 차단 기준 미만은 보고만 하고 진행한다. 보고 내용은 PR 본문에 포함되도록 3단계 prompt에 넘긴다
- 지적을 수용하지 않기로 했으면 **근거를 적는다.** 근거 없이 넘어가지 않는다
- 같은 섹션이 3회 연속 차단되면 사용자에게 보고하고 중단한다

---

## 3단계: PR·CI·머지·배포 (`deploy-runner` phase=publish)

게이트 통과 후 subagent_type="deploy-runner"를 다시 spawn한다.
prompt에 **`phase=publish`** 와 2단계의 리뷰 요약을 포함한다.
subagent의 출력 결과를 그대로 사용자에게 전달한다.

spawn은 **phase당 한 번씩, 총 두 번**이다. `publish`가 결과를 반환하면 파이프라인이 끝난 것이다.
결과가 불완전해 보여도 같은 phase로 다시 spawn하지 않는다.

## Gotchas

- `{type}/{N}` 형태가 아닌 브랜치(develop, main 등)에서 실행하면 파이프라인이 즉시 거부됨 — 브랜치 확인은 subagent spawn 전에 해야 함
- deploy 에이전트를 **phase 없이 spawn하면 안 됨** — 리뷰 게이트를 건너뛰고 PR까지 가버린다
- 리뷰 에이전트를 1단계 **앞에서** 부르면 안 됨 — 아직 커밋 전이라 `git diff develop...HEAD`가 비어 감사 대상이 0건이 된다 (실제로 이 상태로 운영되고 있었음)
- PR merge 후 브랜치 자동 삭제가 GitHub 설정에 따라 실패할 수 있음 — 실패 시 수동 삭제 필요
- CI 통과 후 deploy workflow가 자동으로 트리거되지 않으면 GitHub Actions의 branch trigger 설정을 확인할 것
