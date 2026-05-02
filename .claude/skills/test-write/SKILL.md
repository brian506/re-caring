---
name: test-write
description: 구현된 기능에 대한 테스트 코드를 작성하고 로컬 빌드로 검증한다. 사용자가 "테스트 작성해줘", "테스트 코드 짜줘" 라고 하면 실행.
allowed-tools: Bash(./gradlew *) Bash(git *) Read Grep Glob Edit Write
argument-hint: "[테스트 대상 기능 또는 클래스명]"
---

# Test Write Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

feature-dev 스킬이 구현한 코드를 기반으로 테스트를 작성한다.  
`src/main/` 코드는 수정하지 않는다.

## Step 1: 대상 파악

현재 브랜치의 변경 파일을 확인해 테스트 대상을 파악한다.

```bash
git diff --name-only develop...HEAD -- 'src/main/**'
```

각 파일의 역할(Implement / Business / Controller)을 확인한다.

## Step 2: 기존 테스트 컨벤션 확인

```bash
find src/test -name "*.java" | head -10
```

기존 테스트 파일 중 1~2개를 읽어 Given-When-Then 구조, Mock 방식, Fixture 패턴을 파악한다.

## Step 3: 테스트 작성

### 단위 테스트 (Implement 계층)
- 대상: `*Reader`, `*Writer`, `*Manager`, `*Validator` 등
- Mock 대상: 외부 의존성(Repository, 외부 서비스)만
- Given-When-Then 구조 준수
- 파일 위치: `src/test/java/com/recaring/{domain}/implement/`

### 단위 테스트 (Business 계층)
- 대상: `*Service`
- Mock 대상: Implement 계층 클래스 전체
- 파일 위치: `src/test/java/com/recaring/{domain}/`

### Fixture 클래스
- 테스트 데이터 상수·팩토리 메서드를 `*Fixture` 클래스에 정의
- 동일 도메인의 기존 Fixture가 있으면 메서드를 추가한다 (새 파일 생성 금지)
- 파일 위치: `src/test/java/com/recaring/{domain}/`

### 통합 테스트
- DB 연동이 필요한 경우에만 작성
- `@Tag("integration")` 붙이기

## Step 4: 빌드 & 검증

```bash
./gradlew test 2>&1 | tail -80
```

실패 시 에러 로그를 분석해 수정한다. 3회 반복 후에도 실패하면 사용자에게 보고하고 중단한다.

## 완료 보고

```
작성한 테스트:
  - [파일명 및 테스트 메서드 목록]
빌드 결과: 성공 / 실패
다음 단계: /deploy 로 배포 진행
```
