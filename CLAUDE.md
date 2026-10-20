# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language Rule

**Write all files and code in English.** (variable names, class names, method names, comments, commit messages, PR descriptions, etc.)  
**Always respond to the user in Korean.**

## 프로젝트 분석 지침

개선점, 아키텍처 조언, 전체 프로젝트 분석 요청 시 → **절대로** 전체 코드베이스를 스캔하지 않는다. 아래 파일을 읽는 것으로 충분하다. Explore 에이전트를 쓸 때도 이 파일들만 읽도록 프롬프트에 명시한다.

- **코드 아키텍처 규칙**: `.claude/rules/architecture.md`
- **API·엔티티·기술 부채 현황**: `.claude/skills/feature/references/snapshot.md`

기능 구현 후 새 API·엔티티·알려진 이슈가 생기면 `.claude/skills/feature/references/snapshot.md`의 해당 섹션을 함께 갱신한다.

## 빌드 및 실행 명령어

```bash
docker compose -f docker-compose-local.yml up -d  # 인프라 (PostgreSQL 17, Redis 7)
./gradlew build                # 빌드
./gradlew test                 # 단위 테스트
./gradlew integrationTest      # 통합 테스트 (Testcontainers)
./gradlew test --tests "com.recaring.XxxTest"  # 특정 클래스
./gradlew clean compileJava    # QueryDSL Q클래스 재생성
```

스키마는 Flyway가 관리한다. 로컬 DB 볼륨이 예전 Hibernate `create`로 만든 것이면 `docker compose -f docker-compose-local.yml down -v` 후 다시 띄운다.

로컬 실행 시 Spring 프로파일을 `local`로 지정해야 한다 (`application-local.yml` 참고).

## 스택

Spring Boot 4.0.3 · Java 21 · PostgreSQL 17 · Flyway · Redis 7 · QueryDSL 7 · JWT (jjwt 0.12) · CoolSMS

## 로깅 규칙

로그 메시지는 `[Category : Detail]: key=value | key=value` 형식으로 작성한다. JSON 로그에서 `category`·`detail` 필드로 추출되어 AI 로그 분석이 그룹핑하므로 형식을 엄격히 지킨다.

```java
// 형식: [Category : Detail]: 추가 정보
log.info("[GpsCache : Saved]: wardKey={}", wardKey);
log.warn("[SseEvent : SendClosed]: wardKey={} | error={}", wardKey, e.getMessage());
log.error("[AnomalyResult : ProcessFailed]: wardKey={}", wardKey, e);
```

- Category·Detail은 **영문 PascalCase**만 쓴다 (한국어·공백·대문자 약어 나열 금지). 정규식 `^\[[A-Z][A-Za-z0-9]+ : [A-Z][A-Za-z0-9]+\]`에 맞아야 한다
- Category: 기능·컴포넌트를 가리키는 명사. **고정 어휘 `.claude/rules/log-vocabulary.md`에 있는 값만** 쓰고, 새 기능이면 그 파일에 먼저 추가한다. 도메인(패키지)명은 logger 패키지에서 따로 뽑히므로 도메인명만으로 된 Category(`Location`, `Care`)나 도메인 접두어(`LocationGpsCache`)는 쓰지 않는다. `CareInvitationAlert`처럼 기능 자체의 이름은 허용한다. 동의어를 새로 만들지 않는다 (`AnomalyStream` 하나, `AnomalyStreamEvent` 등 금지)
- Detail: 동작 결과를 나타내는 과거형·결과 구 (예: `Saved`, `SendFailed`, `ConnectionClosed`, `RateLimitExceeded`)
- 추가 정보: `key=value | key=value` 형식으로 컨텍스트 추가
- 레벨 선택 (ERROR는 즉시 알림 대상이므로 엄격히 제한한다)
  - `error`: **사람이 지금 조치해야 하는 상태**. 자동 복구(재시도·다음 주기·PEL 회수)가 끝났거나 없고, 핵심 기능(보호자 알림·이상탐지)·데이터 손실로 이어지는 경우, 또는 예상치 못한 버그(500)
  - `warn`: 실패했지만 재시도·다음 주기·사용자 재시도로 복구 가능한 경우, 외부 의존성 일시 장애·단건 실패. 지속 여부는 지표 비율로 감시한다
  - `info`: 정상 흐름, `debug`: 디버그 (정상적인 SSE 연결 종료 포함)
- 예외를 로깅할 때는 `e.getMessage()` 대신 **예외 객체를 마지막 인자**로 넘긴다 (스택 트레이스·원인 체인 보존). 단, 예외 메시지에 비밀값(웹훅 URL 등)이 담기는 경우는 예외 타입만 남긴다. 잘못된 클라이언트 요청(검증·바인딩 실패)이나 입력 데이터 형식 오류는 스택이 쓸모없으므로 `e.getMessage()`만 남긴다

```java
log.error("[AnomalyResult : ProcessFailed]: wardMemberKey={} | attempt={}", wardMemberKey, attempt, e);
```

- 개인정보는 로그에 평문으로 남기지 않는다. 전화번호는 `MaskingUtils.maskPhone`, 장소 검색어는 길이만, GPS 좌표·토큰은 기록하지 않는다
- 외부 HTTP 호출 예외(`ResourceAccessException` 등)는 메시지에 요청 URL(쿼리 파라미터 포함)이 담기므로 `e.getMostSpecificCause()`를 넘긴다
- `requestId`는 `RequestIdFilter`가 MDC에 넣으므로 메시지에 직접 적지 않는다. dev는 ECS JSON 필드, local은 평문 `[requestId]`로 자동 출력된다
- 비동기 executor는 `AsyncConfig`에서 `MdcTaskDecorator`를 적용해 만든다. 데코레이터가 없는 executor·`CompletableFuture` 공용 풀에서는 `requestId`가 끊긴다

## 인프라 접근 규칙

EC2 서버 접근은 **SSH가 아닌 AWS SSM Session Manager**로만 한다. SSH 키·포트 22를 쓰지 않는다.

인스턴스 ID는 태그(`Name=recaring-app-server`)로 동적 조회한다. 하드코딩 금지.

## 인덱스 규칙

Entity `@Table`의 `indexes` 속성을 사용하지 않는다. 인덱스는 Flyway 마이그레이션(`src/main/resources/db/migration`)에 작성한다. 스키마 변경 규칙은 `.claude/rules/ddl-conventions.md` 참고.

## 환경 선택

prod가 아닌 dev에서만 작업할 것. application-prod.yml에 우선적으로 변경할 것이 아닌 application-dev.yml에 변경할 것