---
name: implement
description: Implements actual feature code following project architecture. Called by the feature skill as a subagent. Reads rules/, then implements DataAccess → Implement → Business → Controller in order.
allowed-tools: AskUserQuestion Bash(./gradlew *) Bash(git *) Bash(bash .claude/skills/feature/scripts/*) Bash(aws *) Bash(find *) Bash(grep *) Bash(gh issue view *) Read Glob Edit Write
---

# Implement Agent

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.
> **금지**: `src/test/` 하위 파일을 절대 건드리지 않는다. 테스트는 `/test-write` 스킬이 전담한다.

## Step 0: 규칙 로드

구현 시작 전 아래 파일을 순서대로 읽는다.

1. `.claude/rules/forbidden.md` — 절대 금지 패턴 (가장 먼저)
2. `.claude/rules/architecture.md` — 레이어 구조, 역할 분리
3. `.claude/rules/patterns.md` — Command / VO / Request 패턴
4. `.claude/rules/error-codes.md` — 에러코드 도메인 범위
5. `.claude/skills/feature/references/snapshot.md` — 현재 API·엔티티 현황 (중복 방지)

## Step 1: 코드베이스 분석 + 설계안 확인 (AskUserQuestion)

아래 항목을 파악한 뒤 **구현 전에 반드시 사용자에게 확인**한다.

분석 항목:
- 어느 도메인 패키지에 속하는지 (`auth` / `care` / `member` / `sms` / `location` / 신규)
- 유사한 기존 API 흐름 (있다면)
- 제안할 API 엔드포인트 (method, path, request, response 초안)
- 필요한 새 Entity / 컬럼 (있다면)
- Implement 계층 역할 구성 (어떤 Reader/Writer/Manager를 만들 것인지)

분석이 끝나면 AskUserQuestion으로 설계안을 제시하고 승인을 받는다:

```
질문 예시:
"아래 설계로 진행할게요. 확인해 주세요.

도메인: {domain}
API: POST /api/v1/{path}
Request: { ... }
Response: { ... }
새 Entity: {EntityName} (컬럼: ...)
Implement: {Reader}, {Writer}, {Validator}

진행할까요?"
```

사용자가 수정 요청하면 반영 후 다시 확인한다. 승인 후 Step 2로 진행한다.

## Step 2: 구현

`.claude/rules/architecture.md`의 레이어 순서대로 구현한다.

### 2-1. DataAccess 계층
Entity, Repository (필요 시 QueryDSL)

### 2-2. DDL 마이그레이션 (새 Entity 생성·컬럼 추가 시 필수)

DDL을 작성한 뒤 **적용 전에 AskUserQuestion으로 확인**한다:

```
질문 예시:
"아래 DDL을 프로덕션 DB에 적용합니다. 확인해 주세요.

{DDL 내용}

적용할까요?"
```

규칙 → `.claude/rules/ddl-conventions.md`

승인 후 실행:

```bash
bash .claude/skills/feature/scripts/apply-ddl.sh "CREATE TABLE ..."
```

`Status: Success`, `Error: ""` 확인 전까지 다음 단계로 진행하지 않는다.

### 2-3. Implement 계층
Reader / Writer / Manager / Validator / Authenticator — 역할 기준 → `.claude/rules/architecture.md`

### 2-4. Request & Command 결정
`.claude/rules/patterns.md`의 "Command 사용 판단" 기준 적용

### 2-5. Business(Service) 계층
흐름 오케스트레이션만 담당. Repository 직접 참조 금지.

### 2-6. Controller 계층
`@AuthMember String memberKey` 파라미터 주입. `toCommand()` 변환 후 Service 호출.

### 2-7. ErrorType / ErrorCode 추가
`.claude/rules/error-codes.md`에서 도메인 범위 확인 후 추가

## Step 3: 스냅샷 갱신

`.claude/skills/feature/references/snapshot.md`를 업데이트한다.

- 새 API → "API 엔드포인트" 테이블
- 새 Entity → "엔티티 목록" 테이블
- 새 도메인 → "도메인별 패키지 현황" 테이블
- 기술 부채 추가/제거 → "알려진 기술 부채" 테이블
- 파일 상단 `마지막 업데이트` 날짜 갱신

## 완료 보고

```
구현 완료: #{N}
구현 항목:
  - [계층별 생성/수정 파일 목록]
```
