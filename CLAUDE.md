# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Rule

**Write all files and code in English.** (variable names, class names, method names, comments, commit messages, PR descriptions, etc.)  
**Always respond to the user in Korean.**

## 프로젝트 분석 지침

개선점, 아키텍처 조언, 전체 프로젝트 분석 요청 시 → **절대로** 전체 코드베이스를 스캔하지 않는다. 아래 파일을 읽는 것으로 충분하다. Explore 에이전트를 쓸 때도 이 파일들만 읽도록 프롬프트에 명시한다.

- **인프라/시스템 구조**: `docs/architecture.md`
- **코드 아키텍처 규칙**: `.claude/skills/feature-dev/references/architecture.md`
- **API·엔티티·기술 부채 현황**: `.claude/skills/feature-dev/references/snapshot.md`

기능 구현 후 새 API·엔티티·알려진 이슈가 생기면 `.claude/skills/feature-dev/references/snapshot.md`의 해당 섹션을 함께 갱신한다.

## 빌드 및 실행 명령어

```bash
docker compose -f docker-compose-local.yml up -d  # 인프라 (PostgreSQL 16, Redis 7)
./gradlew build                # 빌드
./gradlew test                 # 단위 테스트
./gradlew integrationTest      # 통합 테스트 (Testcontainers)
./gradlew test --tests "com.recaring.XxxTest"  # 특정 클래스
./gradlew clean compileJava    # QueryDSL Q클래스 재생성
```

로컬 실행 시 Spring 프로파일을 `local`로 지정해야 한다 (`application-local.yml` 참고).

## 스택

Spring Boot 4.0.3 · Java 21 · PostgreSQL 16 · Redis 7 · QueryDSL 7 · JWT (jjwt 0.12) · CoolSMS

## 로깅 규칙

로그 메시지는 `[카테고리 : 상세내용]` 형식으로 작성한다.

```java
// 형식: [카테고리 : 상세내용]: 추가 정보
log.info("[GPS 수신 : 저장 완료]: wardKey={}", wardKey);
log.warn("[SSE 이벤트 : 전송 실패]: wardKey={} | error={}", wardKey, e.getMessage());
log.error("[Redis 캐시 : 직렬화 실패]: wardKey={} | error={}", wardKey, e.getMessage());
```

- 카테고리: 도메인 또는 컴포넌트명 (예: `GPS 수신`, `SSE 이벤트`, `Redis 캐시`)
- 상세내용: 동작 결과 (예: `저장 완료`, `전송 실패`, `연결 종료`)
- 추가 정보: `key=value | key=value` 형식으로 컨텍스트 추가
- 레벨 선택: 정상 흐름 → `info`, 비즈니스 예외 → `warn`, 시스템 오류 → `error`, 디버그 → `debug`

## 인프라 접근 규칙

EC2 서버 접근은 **SSH가 아닌 AWS SSM Session Manager**로만 한다. SSH 키·포트 22를 쓰지 않는다.

인스턴스 ID는 태그(`Name=recaring-app-server`)로 동적 조회한다. 하드코딩 금지.

## 인덱스 규칙

Entity `@Table`의 `indexes` 속성을 사용하지 않는다. 인덱스는 별도 DDL 쿼리로 관리하며, 필요한 위치에 TODO 주석으로 표시한다.