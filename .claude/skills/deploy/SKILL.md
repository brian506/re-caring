---
name: deploy
description: feature 브랜치에서 빌드/테스트 → 커밋 → PR → CI → 머지 → 배포 확인 → 브랜치 삭제까지 배포 파이프라인을 자동화한다. 사용자가 "커밋해줘", "배포해줘", "PR 올려줘", "deploy" 라고 하면 실행.
allowed-tools: Agent
argument-hint: "[커밋 설명]"
---

# Deploy

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

`deploy` subagent를 생성하여 현재 feature 브랜치의 변경사항을 배포한다.

Agent tool을 사용해 subagent_type="deploy"로 spawn하고, 커밋 설명을 prompt에 포함한다.
subagent의 출력 결과를 그대로 사용자에게 전달한다.
