---
name: feature-dev
description: Implements new features following project architecture rules (issue → branch → code). Does NOT write tests — use /test-write for that. Trigger on: '~API 만들어줘', '~기능 구현해줘', '~추가해줘', '~만들어줘'.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Bash(aws *) Read Grep Glob Edit Write
argument-hint: "[구현할 기능 설명]"
---

# Feature Dev Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

> **금지**: 이 스킬은 테스트 코드를 절대 작성하지 않는다. `src/test/` 하위 파일을 건드리지 않는다. 테스트는 `/test-write` 스킬이 전담한다.

사용자가 전달한 기능 설명을 기반으로 아래 단계를 순서대로 실행한다.

## Step 0: 이슈 확인 및 생성

기존 이슈 중 관련된 것이 있는지 확인 후 재사용 또는 신규 생성한다.

```bash
gh issue list --state open --limit 50
bash scripts/create-issue.sh "{이슈 제목}" "{이슈 본문}"
```

반환된 이슈 번호(N)를 이후 모든 단계에 사용한다.

## Step 1: Feature 브랜치 생성

```bash
bash scripts/setup-branch.sh {N}
```

## Step 2: 코드베이스 분석

- 어느 도메인 패키지에 속하는지 (`auth` / `care` / `member` / `sms` / 신규)
- 유사한 기존 API가 있다면 Controller → Business → Implement 흐름 확인
- 사용할 ErrorCode 범위 확인 → `references/error-codes.md`

## Step 3: 구현

`references/architecture.md`를 읽고 아래 순서로 구현한다.

### 3-1. DataAccess 계층
Entity, Repository (필요 시 QueryDSL)

### 3-2. DDL 마이그레이션 (새 Entity 생성 · 컬럼 추가 시 필수)

**배포 전에 반드시 프로덕션 DB에 먼저 적용한다.** DDL 작성 규칙 → `references/ddl-conventions.md`

```bash
bash .claude/skills/feature-dev/scripts/apply-ddl.sh "CREATE TABLE ..."
```

`Status: Success`, `Error: ""` 확인 전까지 다음 단계로 넘어가지 않는다.

### 3-3. Implement 계층
Reader / Writer / Manager / Validator / Authenticator — 역할 판단 기준 → `references/architecture.md`

### 3-4. Request & Command 결정
`references/patterns.md`의 "Command 사용 판단" 섹션 참고

### 3-5. Business(Service) 계층
흐름 오케스트레이션만 담당. Repository 직접 참조 금지.

### 3-6. Controller 계층
`@AuthMember String memberKey` 파라미터 주입. `toCommand()` 변환 후 Service 호출.

### 3-7. ErrorType / ErrorCode 추가
`references/error-codes.md`에서 다음 사용 가능한 코드 번호 확인 후 추가

## Step 4: 스냅샷 문서 갱신

변경 사항을 `references/snapshot.md`에 반영한다. 변경 없으면 건너뛴다.

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

## Gotchas

- DDL 미적용 상태에서 코드를 배포하면 런타임 오류 발생 — apply-ddl.sh의 `Status: Success` 확인 전까지 다음 단계 진행 금지
- snapshot.md 갱신을 빠뜨리면 다음 기능 구현 시 중복 API·엔티티가 생성될 위험이 있음
- 동일 도메인의 기존 Fixture 클래스가 있는데 새 파일을 만들면 컨벤션 위반 — 반드시 기존 파일에 메서드 추가
- Business 계층에서 Repository를 직접 참조하면 아키텍처 위반 — Implement 계층을 반드시 경유

---

테스트 코드 작성·빌드 검증은 `/test-write` 스킬을 사용한다.
