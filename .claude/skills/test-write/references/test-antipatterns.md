# 테스트 안티패턴 체크리스트

**읽는 시점: 테스트를 작성한 뒤의 자기검토.**
작성 전 설계 지침은 `test-design.md`를 본다. 두 문서는 역할이 다르다.

2026-08-25 전 도메인 테스트 감사에서 **반복적으로** 발견된 실패 형태다.
당시 `./gradlew test`는 100% 통과 중이었고, 아래 패턴들은 전부 "통과하지만 버그를 못 잡는" 테스트였다.

작성한 테스트를 아래 목록과 순서대로 대조하고, 해당하는 항목이 있으면 **번호와 함께** 보고한다.
구체적 사례는 `audit/audit-findings-2026-08.md` 참고.

---

## 1. `any()` 로 인자를 흘리지 않는다 — 가장 흔하고 가장 위험

`verify`/`then`에서 인자를 `any()`, `anyString()`으로 두면 **호출 여부만** 검증된다.
인자가 뒤바뀌거나 엉뚱한 값이 들어가도 통과한다.

```java
// 나쁨 — 현재/새 비밀번호를 뒤바꿔도 통과한다
then(authenticator).should().verifyPassword(anyString(), any(Password.class));

// 좋음 — 값을 고정한다
then(authenticator).should().verifyPassword(MEMBER_KEY, new Password(CURRENT_PASSWORD));
```

객체를 통째로 넘길 때는 `ArgumentCaptor`로 열어서 **필드별로** 단언한다.
특히 다음은 반드시 값을 고정한다:

- 인증·인가에 쓰이는 key (`memberKey`, `wardKey`, `caregiverKey`)
- 비밀번호의 current/new 구분
- 알림 title/body/dataPayload
- 좌표 latitude/longitude (스왑 회귀가 잦다)
- 시각 필드 `recordedAt` vs `measuredAt`

## 2. 테스트 데이터를 서로 다르게 준다

같은 값을 두 자리에 넣으면 스왑 버그가 드러나지 않는다.

```java
// 나쁨 — 두 인자가 같은 값이라 뒤바뀌어도 통과
service.updateMyInfo(key, RAW_PASSWORD, RAW_PASSWORD);

// 좋음
service.updateMyInfo(key, "current1", "newpass2");
```

**기본값과 같은 값으로 "변경"을 테스트하지 않는다.** 예: `LocationCollectionInterval.DEFAULT`가
30초인데 PATCH로 30을 넣고 GET에서 30을 확인하면, 핸들러 본문을 통째로 지워도 통과한다.
반드시 기본값이 **아닌** 값을 쓴다.

## 3. 스텁한 값을 되받는 동어반복을 만들지 않는다

```java
// 무의미 — mock이 준 걸 그대로 비교한다
given(reader.find(key)).willReturn(expected);
assertThat(service.get(key)).isEqualTo(expected);
```

Service가 순수 위임 계층이라 검증할 로직이 없다면 **테스트를 쓰지 않는다.**
커버리지 숫자만 올리고 잡는 버그는 "메서드 이름 오타"뿐이다.
그 대신 실제 로직이 있는 Implement 계층이나 Repository 쿼리를 테스트한다.

같은 이유로 **스텁한 호출을 그대로 verify 하지 않는다**
(`given(x.f()).willReturn(v)` 후 `then(x).should().f()`).

## 4. 모순된 mock 조합을 만들지 않는다 — 죽은 코드를 은폐한다

실 DB에서 동시에 성립할 수 없는 상태를 stub하면, 프로덕션에서 절대 성공할 수 없는
코드 경로가 초록불로 덮인다.

```java
// 실제로 이 조합이 "항상 실패하는 API"를 6개월간 감췄다
given(repo.existsCareRelationship(WARD, GUARDIAN, GUARDIAN)).willReturn(true);  // 관계가 있다
given(repo.findAllByWardMemberKey(WARD)).willReturn(List.of());                 // 관계가 없다
```

스텁을 여러 개 세울 때 **"이 상태가 실제 DB에서 가능한가"** 를 자문한다.
같은 데이터를 두 경로로 조회하는 코드라면 두 스텁이 서로 모순되지 않아야 한다.

## 5. 부수효과를 검증한다 — HTTP 상태만 보지 않는다

통합 테스트에서 200/`resultType: SUCCESS`만 확인하면 **서비스가 아무 일도 안 해도 통과**한다.

- POST/PATCH → 저장된 행을 다시 읽어 값이 반영됐는지
- DELETE → 조회했을 때 없는지, 그리고 **다른 행이 지워지지 않았는지**
- 회원가입 → 비밀번호가 해시(`$2a$`)로 저장됐는지
- GPS 수신 → 히스토리 행이 생겼는지

`is4xxClientError()` 대신 정확한 상태와 에러 코드를 단언한다.
좋은 예: `.expectStatus().isBadRequest()` + `jsonPath("$.error.errorCode").isEqualTo("E6002")`

## 6. 절대 실패할 수 없는 단언을 쓰지 않는다

- 메서드 안에서 `new`로 만들어 반환하는 객체에 `isNotNull()`
- 생성자가 `UUID.randomUUID()`로 채우는 필드에 `isNotBlank()`
- 생성자가 `LocalDateTime.now()`로 채우는 필드에 `isNotNull()`
- 본문이 `log.debug()` 한 줄인 void 메서드에 `doesNotThrowAnyException()`
- **빈 컬렉션에 대한 `allMatch`/`noneMatch`** — AssertJ에서 무조건 통과한다.
  단언 전에 `assertThat(result).hasSize(n)` 으로 비어 있지 않음을 먼저 고정한다.

`doesNotThrowAnyException()`이 Validator 테스트의 **유일한** 단언이면,
빈 validator 본문으로도 통과하는지 자문한다. 통과한다면 협력 객체 호출을 verify로 추가한다.

## 7. 순서가 보안에 영향을 주면 `InOrder`로 고정한다

권한 검증이 부수효과보다 **먼저** 일어나야 하는 경우가 많다.
`then(a).should()` + `then(b).should()`는 순서를 고정하지 않는다.

```java
InOrder inOrder = inOrder(validator, manager);
inOrder.verify(validator).validateGuardianAccess(requesterKey, wardKey);
inOrder.verify(manager).update(...);
```

추가로 **검증 실패 시 부수효과가 일어나지 않았는지** 반례를 넣는다:
`willThrow(...).given(validator).validate(...)` 후 `then(manager).should(never()).update(...)`.

## 8. 경계값을 넣는다

한도·만료·범위 검사는 **경계 양쪽**을 모두 테스트한다.

- 한도 N: `N-1`(통과)과 `N`(실패) 둘 다. 실패 케이스만 있으면
  `count >= max` → `count >= max - 1` 회귀를 못 잡는다.
- 날짜 범위: 시작 정각(포함), 종료 직전(포함), 종료 정각(제외), 시작 직전(제외)
- 정규식: 자릿수 -1 / +1, 허용되지 않는 접두사, 하이픈·공백 포함
- 만료: 정확히 만료 시각, 그 ±1초

## 9. 비동기 검증에 `never()`를 그냥 쓰지 않는다

`@Async` 리스너나 별도 스레드에서 도는 코드에 `verify(mock, never())`를 즉시 걸면,
**비동기 작업이 시작조차 하기 전에** 검증이 끝나 무조건 통과한다.

```java
// 나쁨 — 로직이 깨져도 통과한다
verify(historyManager, never()).findLatest(key);

// 좋음
verify(historyManager, after(500).never()).findLatest(key);
```

또한 테스트에서 무한 폴링 루프를 시작시켰다면 **반드시 종료시킨다**
(`emitter.complete()` 등). 안 그러면 스레드가 누수되어 테스트 종료 후에도 mock을 계속 호출한다.

## 10. `@DisplayName`이 주장하는 것을 실제로 단언한다

이름이 "케어 관계가 생성된다"인데 단언이 HTTP 200뿐이거나,
"오름차순으로 조회한다"인데 정렬 단언이 없거나,
"MANAGER도 조회한다"인데 GUARDIAN과 동일한 스텁을 쓰는 경우가 반복됐다.

이름과 단언이 어긋나면 **이름을 고치는 게 아니라 단언을 추가한다.**

---

## 실행 관련

- `./gradlew test` 는 `excludeTags 'integration'` 이라 **통합 테스트를 실행하지 않는다**
  (`build.gradle:98`). 통합 테스트는 `./gradlew integrationTest` 로 별도 실행한다.
- 2026-08-25 기준 CI(`.github/workflows/ci-check.yml:32`, `deploy.yml:46`)는
  `./gradlew clean test` 만 돌린다. **통합 테스트는 CI에서 한 번도 실행되지 않는다.**
  그 결과 이미 깨진 통합 테스트가 방치된 상태였다. 통합 테스트를 새로 쓸 때는
  로컬에서 반드시 `./gradlew integrationTest`로 직접 확인한다 (Docker 필요).
- `AbstractIntegrationTest`·`AbstractRepositoryTest` 둘 다 `@Tag("integration")`을 상속시킨다.
  이 둘을 상속하지 않고 DB에 붙는 테스트를 만들면 태그를 직접 붙여야 한다.
- git bash의 `JAVA_HOME`이 JVM 8을 가리켜 gradle이 실패할 수 있다.
  `JAVA_HOME="C:/Users/SSAFY/.jdks/ms-21.0.12" ./gradlew test`

## 커버리지 공백을 먼저 확인한다

새 테스트를 쓰기 전에, 테스트 파일이 아예 없는 프로덕션 클래스를 먼저 찾는다.

```bash
for f in $(find src/main/java/com/recaring -path "*implement*" -name "*.java" -o -path "*business*" -name "*.java" | grep -v "/vo/"); do
  n=$(basename $f .java)
  if [ -z "$(find src/test -name "${n}Test.java")" ]; then echo "$n <- $f"; fi
done
```

특히 **인증 필터, Validator, 외부 연동 클라이언트**에 테스트가 없으면 우선순위를 높인다.
감사 시점에 `JwtAuthenticationFilter`, `DeviceTokenAuthFilter`, `MemberValidator`,
`FirebaseFcmClient`, `GpsHistoryManager`가 전부 테스트 0건이었다.
