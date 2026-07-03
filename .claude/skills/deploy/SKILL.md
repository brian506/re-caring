---
name: deploy
description: Automates branch deployment pipeline (commit → PR → CI → merge → deploy). Runs on issue-linked branches of the form {type}/{N} (feature, refactor, fix, chore, hotfix, ...). Trigger on: '배포해줘', '커밋해줘', 'PR 올려줘', 'deploy', 'merge해줘'.
allowed-tools: Agent Bash(git branch *) Bash(gh issue view *)
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

`security-auditor` subagent를 먼저 spawn하여 보안 취약점을 검사한다.

Agent tool을 사용해 subagent_type="security-auditor"로 spawn한다.
prompt에는 현재 브랜치명과 "변경된 파일의 보안 취약점을 감사하고 배포 판정을 내려줘"를 포함한다.

### 보안 감사 결과에 따른 분기

- **🔴 Critical 또는 🟠 High 발견 시**: 배포를 즉시 중단하고 아래 메시지를 출력한다. deploy subagent를 spawn하지 않는다.
  ```
  [배포 차단] 보안 취약점이 발견되었습니다.
  Critical/High 항목을 수정한 뒤 다시 배포해주세요.
  ```
- **🟡 Medium만 발견 시**: 발견 내용을 출력하고 사용자에게 계속 진행할지 확인을 요청한다.
- **이상 없거나 🔵 Low만 발견 시**: 보안 감사 통과 메시지를 출력하고 다음 단계로 진행한다.

---

보안 감사 통과 후 `deploy` subagent를 생성하여 현재 브랜치의 변경사항을 배포한다.

Agent tool을 사용해 subagent_type="deploy"로 spawn하고, 커밋 설명을 prompt에 포함한다.
subagent의 출력 결과를 그대로 사용자에게 전달한다.

## Gotchas

- `{type}/{N}` 형태가 아닌 브랜치(develop, main 등)에서 실행하면 파이프라인이 즉시 거부됨 — 브랜치 확인은 subagent spawn 전에 해야 함
- PR merge 후 브랜치 자동 삭제가 GitHub 설정에 따라 실패할 수 있음 — 실패 시 수동 삭제 필요
- CI 통과 후 deploy workflow가 자동으로 트리거되지 않으면 GitHub Actions의 branch trigger 설정을 확인할 것
