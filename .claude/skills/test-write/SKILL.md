---
name: test-write
description: Writes unit and integration tests for implemented features and verifies with local build. Does not modify src/main/. Trigger on: '테스트 작성해줘', '테스트 코드 짜줘', '테스트 써줘'.
allowed-tools: Bash(./gradlew *) Bash(git *) Read Grep Glob Edit Write Agent
argument-hint: "[테스트 대상 기능 또는 클래스명]"
---

# Test Write Pipeline

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

구현된 코드에 대한 테스트를 작성한다. `src/main/` 코드는 수정하지 않는다.

## 참고 문서 — 읽는 시점이 정해져 있다

| 문서 | 읽는 시점 |
|------|----------|
| `references/test-design.md` | **Step 3 시작 전 (필수)** — 무엇을 어떻게 쓸지 |
| `references/test-antipatterns.md` | Step 5에서 **`test-review` 에이전트가** 읽는다. 이쪽에서 미리 읽지 않는다 |
| `references/test-conventions.md` | Step 4 (필수) — 형식·파일 위치·네이밍·기댓값 출처 주석 |
| `references/audit/` | 기존 테스트를 보강할 때만. 2026-08-25 시점 스냅샷이라 **먼저 유효성을 확인**할 것 |

---

## Step 1: 대상 파악

```bash
git diff --name-only develop...HEAD -- 'src/main/**'
```

각 파일의 계층(VO / Implement / Business / Controller)을 확인한다.

## Step 2: 스펙 정리

대상 기능의 **입력·출력·제약**을 3~5줄로 정리해 사용자에게 제시한다.

근거는 이슈·요구사항·API 명세에서 가져온다.
**구현 코드를 읽어 역산하지 않는다** — 그렇게 하면 버그를 정답으로 고정하게 된다.
근거가 없어 확인이 필요한 항목은 추측하지 말고 질문한다.

## Step 3: 케이스 목록 제시 — 중단점

> 시작 전 `references/test-design.md`를 읽는다.

테스트 **제목 목록만** 작성해 제시한다. 코드는 아직 쓰지 않는다.
정상 / 경계 / 예외 / 결정성 4분류를 훑고, 빠진 분류가 있으면 이유를 함께 적는다.

```
- 발송 24시간 이내에는 수락할 수 있다              (정상)
- 발송 후 정확히 24시간이면 수락할 수 없다          (경계)
- 만료된 초대를 수락하면 CARE_INVITATION_EXPIRED    (예외)
```

**사용자가 목록을 승인할 때까지 다음 단계로 넘어가지 않는다.**
경계값·예외 케이스 추가 지시를 받으면 목록에 반영해 다시 제시한다.

## Step 4: 작성

승인된 목록의 각 항목을 테스트로 옮긴다. 목록에 없는 테스트를 임의로 추가하지 않는다.

- 파일 위치·네이밍·Fixture 규칙: `references/test-conventions.md`
- 동일 도메인에 기존 `*Fixture`가 있으면 **새 파일을 만들지 말고 메서드를 추가**한다
- 통합 테스트에는 `@Tag("integration")`을 붙인다

## Step 5: 검증 — 별도 컨텍스트

`test-review` 에이전트를 호출한다. 직접 자기검토하지 않는다.

작성자와 같은 컨텍스트에서 검토하면 자기 결정에 대한 정당화가 이미 쌓여 있어
스스로 만든 선택은 걸러지지 않는다. 그래서 검증은 다른 모델·빈 컨텍스트에서 돌린다.

```
Agent(subagent_type: "test-review", run_in_background: false)
프롬프트에 포함할 것:
  - 검토 대상 테스트 파일 경로
  - 대응하는 src/main 클래스 경로
  - Step 2에서 정리한 스펙
```

에이전트는 **읽기 전용이라 코드를 고치지 않는다.** 돌아온 지적을 받아 이쪽에서 수정한다.

- 지적을 수용해 고쳤으면 항목 번호와 조치를 보고한다
- 수용하지 않기로 했으면 **근거를 적는다.** 근거 없이 무시하지 않는다
- `판정: 수정 필요`인 채로 Step 6에 넘어가지 않는다

## Step 6: 실행

```bash
./gradlew test 2>&1 | tail -80
```

`JAVA_HOME`이 JVM 8을 가리켜 실패하면 JDK 21을 명시한다:
`JAVA_HOME="C:/Users/SSAFY/.jdks/ms-21.0.12" ./gradlew test`

**통합 테스트를 작성했다면 반드시 별도로 실행한다** — `./gradlew test`는
`excludeTags 'integration'`이라 실행되지 않고, CI도 통합 테스트를 돌리지 않는다.

```bash
./gradlew integrationTest 2>&1 | tail -80   # Docker 필요
```

실패 시 로그를 분석해 수정한다. **테스트를 통과시키려고 `src/main/`을 고치지 않는다** —
구현 버그로 판단되면 수정하지 말고 보고한다 (`/feature` 범위).
반복 실패 시 사용자에게 보고하고 중단한다.

---

## 완료 보고

```
스펙: [3~5줄 요약]

작성한 테스트:
  - [파일명] — [테스트 메서드 목록]

test-review 판정: 통과 / 수정 필요 → 조치 후 재검증
  - [#N] 지적과 조치
  - [#N] 수용하지 않음 — 근거

빌드 결과: 성공 / 실패
발견한 구현 버그: [있으면 파일:줄과 내용 / 없으면 없음]
```
