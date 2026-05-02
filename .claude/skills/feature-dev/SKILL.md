---
name: feature-dev
description: 이 프로젝트의 아키텍처 규칙을 따라 새 기능을 구현한다. 이슈 확인/생성 → 브랜치 → 코드 구현까지 수행. 테스트·빌드는 별도 명령으로 분리. 사용자가 "~API 만들어줘", "~기능 구현해줘" 라고 하면 실행.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Read Grep Glob Edit Write
argument-hint: "[구현할 기능 설명]"
---

# Feature Dev Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

> **금지**: 이 스킬은 테스트 코드를 절대 작성하지 않는다. `src/test/` 하위 파일을 건드리지 않는다. 테스트는 `/test-write` 스킬이 전담한다.

사용자가 전달한 기능 설명을 기반으로 아래 단계를 순서대로 실행한다.

## Step 0: 이슈 확인 및 생성

먼저 기존 이슈 중 관련된 것이 있는지 확인한다.

```bash
gh issue list --state open --limit 50
```

유사한 이슈가 있으면 사용자에게 보여주고 해당 이슈를 재사용할지 새로 만들지 확인한다.
새로 만드는 경우 `assets/issue-template.md`가 있으면 참고, 없으면 아래 형식으로 작성한다.

```bash
bash scripts/create-issue.sh "{이슈 제목}" "{이슈 본문}"
```

반환된 이슈 번호(N)를 이후 모든 단계에 사용한다.

## Step 1: Feature 브랜치 생성

```bash
bash scripts/setup-branch.sh {N}
```

## Step 2: 코드베이스 분석

구현 전에 아래를 파악한다.

- 어느 도메인 패키지에 속하는지 (`auth` / `care` / `member` / `sms` / 신규)
- 유사한 기존 API가 있다면 Controller → Business → Implement 흐름 확인
- 사용할 ErrorCode 범위 확인 → `references/error-codes.md` 참고

## Step 3: 구현

`references/architecture.md`를 읽고 아래 순서로 구현한다.

### 3-1. DataAccess 계층
- Entity, Repository (필요 시 QueryDSL)

### 3-2. Implement 계층
- Reader / Writer / Manager / Validator / Authenticator 역할에 맞게 분리
- 역할 판단 기준 → `references/architecture.md`

### 3-3. Request & Command 결정
- `references/patterns.md`를 읽고 "Command 사용 판단" 섹션에 따라 결정

### 3-4. Business(Service) 계층
- 흐름 오케스트레이션만 담당. Repository 직접 참조 금지.

### 3-5. Controller 계층
- `@AuthMember String memberKey` 파라미터 주입
- `toCommand()` 변환 후 Service 호출 (Command 쓰는 경우에만)

### 3-6. ErrorType / ErrorCode 추가
- `references/error-codes.md` 에서 다음 사용 가능한 코드 번호 확인 후 추가

## Step 4: 스냅샷 문서 갱신

구현으로 인해 변경된 내용을 `.claude/skills/feature-dev/references/snapshot.md` 에 반영한다.

- 새 API 엔드포인트 → "API 엔드포인트" 테이블에 추가
- 새 Entity → "엔티티 목록" 테이블에 추가
- 새 도메인 → "도메인별 패키지 현황" 테이블에 추가
- 해결된 기술 부채 → "알려진 기술 부채" 테이블에서 제거
- 새롭게 발견한 기술 부채 → "알려진 기술 부채" 테이블에 추가
- 파일 상단 `마지막 업데이트` 날짜를 오늘 날짜로 수정

변경 사항이 없으면 이 단계를 건너뛴다.

## 완료 보고

구현한 내용을 아래 형식으로 요약한다.

```
이슈: #{N}
브랜치: feature/{N}
구현 항목:
  - [계층별 생성/수정 파일 목록]
다음 단계: 테스트·빌드 준비되면 "테스트 작성해줘" 또는 /deploy 로 배포 진행
```

---

테스트 코드 작성·빌드 검증은 `/test-write` 스킬을 사용한다.
