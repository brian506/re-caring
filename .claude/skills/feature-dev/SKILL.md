---
name: feature-dev
description: 이 프로젝트의 아키텍처 규칙을 따라 새 기능을 구현한다. 이슈 생성 → 브랜치 → 코드 구현 → 테스트 작성 → 빌드 검증까지 수행. 사용자가 "~API 만들어줘", "~기능 구현해줘" 라고 하면 실행.
allowed-tools: Bash(./gradlew *) Bash(git *) Bash(gh *) Bash(bash *) Read Grep Glob Edit Write
argument-hint: "[구현할 기능 설명]"
---

# Feature Dev Pipeline

사용자가 전달한 기능 설명을 기반으로 아래 단계를 순서대로 실행한다.

## Step 0: 이슈 생성

`assets/issue-template.md`가 있으면 참고, 없으면 아래 형식으로 이슈 제목·본문을 직접 작성한다.

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

`references/architecture.md`와 `references/patterns.md`를 읽고 아래 순서로 구현한다.

### 3-1. DataAccess 계층
- Entity, Repository (필요 시 QueryDSL)

### 3-2. Implement 계층
- Reader / Writer / Manager / Validator / Authenticator 역할에 맞게 분리
- 역할 판단 기준 → `references/architecture.md`

### 3-3. Request & Command 결정
- **Command 사용 여부** → `references/patterns.md` 의 "Command 사용 판단" 섹션 반드시 참고

### 3-4. Business(Service) 계층
- 흐름 오케스트레이션만 담당. Repository 직접 참조 금지.

### 3-5. Controller 계층
- `@AuthMember String memberKey` 파라미터 주입
- `toCommand()` 변환 후 Service 호출 (Command 쓰는 경우에만)

### 3-6. ErrorType / ErrorCode 추가
- `references/error-codes.md` 에서 다음 사용 가능한 코드 번호 확인 후 추가

## Step 4: 테스트 작성

`references/test-conventions.md`를 읽고 아래를 작성한다.

- **단위 테스트**: Business 계층 + Implement 계층 각 1개 이상
- **Fixture 클래스**: 테스트 데이터 상수·팩토리 메서드 정의
- 통합 테스트는 DB 연동이 필요한 경우에만 작성

## Step 5: 로컬 빌드 & 테스트 검증

```bash
./gradlew clean build
```

실패 시 에러 로그를 분석해 코드를 수정한다. 3회 반복 후에도 실패하면 사용자에게 보고하고 중단한다.

## 완료 보고

구현한 내용을 아래 형식으로 요약한다.

```
이슈: #{N}
브랜치: feature/{N}
구현 항목:
  - [계층별 생성/수정 파일 목록]
테스트:
  - [작성한 테스트 클래스 목록]
다음 단계: /deploy 로 배포 진행
```
