---
name: feature
description: Implements new features following project architecture rules (issue → branch → code). Does NOT write tests — use /test-write for that. Trigger on: '~API 만들어줘', '~기능 구현해줘', '~추가해줘', '~만들어줘'.
allowed-tools: AskUserQuestion Bash(git branch *) Bash(git *) Bash(gh issue *) Bash(gh issue list *) Bash(./gradlew *) Bash(bash .claude/skills/feature/scripts/*) Bash(aws *) Bash(find *) Bash(grep *) Read Glob Edit Write
argument-hint: "[구현할 기능 설명]"
---

# Feature Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.
> **금지**: `src/test/` 하위 파일을 절대 건드리지 않는다. 테스트는 `/test-write` 스킬이 전담한다.

사용자가 전달한 기능 설명을 기반으로 아래 단계를 순서대로 **메인 컨텍스트에서 직접** 실행한다. (서브에이전트 위임하지 않는다)

## Step 0: 이슈 확인 및 생성

기존 이슈 중 관련된 것이 있는지 확인 후 재사용 또는 신규 생성한다.

```bash
gh issue list --state open --limit 50
```

관련 이슈가 없으면 생성한다:

```bash
bash .claude/skills/feature/scripts/create-issue.sh "<feature|bug|refactor>" "제목 (prefix 제외)" "설명" "작업항목"
```

이슈 유형별 `.github/ISSUE_TEMPLATE/{type}.yml` 형식이 자동 적용된다:
- `feature`  → `[Feature]: 제목`  / 📄 설명 + ✅ 작업할 내용
- `bug`      → `[Bug]: 제목`      / 📄 설명 + ✅ 기대 동작 + 🖼️ 첨부 자료
- `refactor` → `[Refactor]: 제목` / 📄 설명 + ✅ 작업할 내용

반환된 이슈 번호(N)를 이후 모든 단계에 사용한다.

## Step 1: Feature 브랜치 생성

```bash
bash .claude/skills/feature/scripts/setup-branch.sh {N}
```

## Step 2: 규칙 로드

구현 시작 전 아래 파일을 순서대로 읽는다.

1. `.claude/rules/forbidden.md` — 절대 금지 패턴 (가장 먼저)
2. `.claude/rules/architecture.md` — 레이어 구조, 역할 분리
3. `.claude/rules/patterns.md` — Command / VO / Request 패턴
4. `.claude/rules/error-codes.md` — 에러코드 도메인 범위
5. `.claude/skills/feature/references/snapshot.md` — 현재 API·엔티티 현황 (중복 방지)

## Step 3: 코드베이스 분석 + 설계안 확인 (AskUserQuestion)

아래 항목을 파악한 뒤 **구현 전에 반드시 사용자에게 확인**한다. (이미 대화에서 합의된 설계라면 생략 가능)

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

사용자가 수정 요청하면 반영 후 다시 확인한다. 승인 후 Step 4로 진행한다.

## Step 4: 구현

`.claude/rules/architecture.md`의 레이어 순서대로 구현한다.

### 4-1. DataAccess 계층
Entity, Repository (필요 시 QueryDSL)

### 4-2. Flyway 마이그레이션 작성 (새 Entity·컬럼·타입·제약·인덱스 변경 시 필수)

엔티티를 바꾸면 같은 브랜치에 `src/main/resources/db/migration/V{n}__{description}.sql`을 추가한다.
DB에 직접 적용하지 않는다. local·test·dev 모두 앱 기동 시 Flyway가 적용한다.

- 버전 `n`은 develop의 최댓값 + 1. 이미 있는 마이그레이션 파일은 수정하지 않는다.
- 엔티티에서 실제로 바뀐 것만 정확히 기록한다. 특히 다음을 빠뜨리지 않는다:
  - 새 테이블/컬럼 추가 (`CREATE TABLE`, `ALTER TABLE ... ADD COLUMN`)
  - **삭제된 컬럼** (`ALTER TABLE ... DROP COLUMN`) — 단, 이전 버전 코드가 쓰는 컬럼이면 expand → contract로 다음 배포에 분리
  - **삭제된 테이블** (`DROP TABLE`) — FK로 참조되면 참조 테이블부터 먼저 정리
  - 타입/NULL/UNIQUE 제약 변경 (`ALTER TABLE ... ALTER COLUMN`)
  - 인덱스 (`CREATE INDEX`)
- `safe_zones`·`gps_histories` 변경은 이상탐지 엔진에 영향이 있으므로 완료 보고에 명시한다.
- 변환 규칙·무중단 제약 → `.claude/rules/ddl-conventions.md`
- 엔티티와 SQL이 맞는지는 `./gradlew integrationTest`가 Hibernate validate로 확인한다.

### 4-3. Implement 계층
Reader / Writer / Manager / Validator / Authenticator — 역할 기준 → `.claude/rules/architecture.md`

### 4-4. Request & Command 결정
`.claude/rules/patterns.md`의 "Command 사용 판단" 기준 적용

### 4-5. Business(Service) 계층
흐름 오케스트레이션만 담당. Repository 직접 참조 금지.

### 4-6. Controller 계층
`@AuthMember String memberKey` 파라미터 주입. `toCommand()` 변환 후 Service 호출.

### 4-7. ErrorType / ErrorCode 추가
`.claude/rules/error-codes.md`에서 도메인 범위 확인 후 추가

### 4-8. 컴파일 확인

```bash
./gradlew compileJava
```

`BUILD SUCCESSFUL` 확인 전까지 완료로 보고하지 않는다.

## Step 5: 스냅샷 갱신

`.claude/skills/feature/references/snapshot.md`를 업데이트한다.

- 새 API → "API 엔드포인트" 테이블
- 새 Entity → "엔티티 목록" 테이블
- 새 도메인 → "도메인별 패키지 현황" 테이블
- 기술 부채 추가/제거 → "알려진 기술 부채" 테이블
- 파일 상단 `마지막 업데이트` 날짜 갱신

## 완료 보고

```
이슈: #{N}
브랜치: feature/{N}
구현 항목:
  - [계층별 생성/수정 파일 목록]
다음 단계: 테스트·빌드 준비되면 "테스트 작성해줘" 또는 /deploy 로 배포 진행
```
