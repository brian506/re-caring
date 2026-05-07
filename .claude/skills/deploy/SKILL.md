---
name: deploy
description: Automates feature branch deployment pipeline (commit → PR → CI → merge → deploy). Only runs on feature/{N} branches. Trigger on: '배포해줘', '커밋해줘', 'PR 올려줘', 'deploy', 'merge해줘'.
allowed-tools: Agent Bash(git branch *)
argument-hint: "[커밋 설명]"
---

# Deploy

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

## 사전 브랜치 확인 (필수 — 실패 시 즉시 중단)

subagent를 spawn하기 전에 반드시 현재 브랜치를 확인한다.

```bash
git branch --show-current
```

출력값이 `feature/숫자` 형태(예: `feature/42`)가 **아니라면** 즉시 중단하고 아래 메시지를 사용자에게 출력한 뒤 파이프라인을 종료한다. **어떠한 경우에도 다음 단계로 넘어가지 않는다.**

```
[배포 중단] 현재 브랜치가 feature/{N} 형태가 아닙니다. (현재: {브랜치명})
/deploy는 feature 브랜치에서만 사용할 수 있습니다.

- 새 기능 개발: /feature-dev 로 이슈·브랜치를 먼저 생성하세요.
- 긴급 수정: /hotfix 를 사용하세요.
```

브랜치가 `feature/{N}` 형태인 경우에만 아래를 계속 진행한다.

---

`deploy` subagent를 생성하여 현재 feature 브랜치의 변경사항을 배포한다.

Agent tool을 사용해 subagent_type="deploy"로 spawn하고, 커밋 설명을 prompt에 포함한다.
subagent의 출력 결과를 그대로 사용자에게 전달한다.

## Gotchas

- `feature/{N}` 형태가 아닌 브랜치(develop, main 등)에서 실행하면 파이프라인이 즉시 거부됨 — 브랜치 확인은 subagent spawn 전에 해야 함
- PR merge 후 브랜치 자동 삭제가 GitHub 설정에 따라 실패할 수 있음 — 실패 시 수동 삭제 필요
- CI 통과 후 deploy workflow가 자동으로 트리거되지 않으면 GitHub Actions의 branch trigger 설정을 확인할 것
