# 테스트 설계 지침

**읽는 시점: 테스트를 작성하기 전.**
작성한 뒤의 자기검토는 `test-antipatterns.md`로 한다. 두 문서는 역할이 다르다.

---

## 0. 케이스 도출 — 코드보다 제목을 먼저 쓴다

구현 코드를 보고 테스트를 짜면 구현을 그대로 미러링하게 된다.
**스펙을 3~5줄로 정리 → 케이스 제목 목록 제시 → 승인 → 코드 작성** 순서를 지킨다.

제목은 아래 4분류로 훑어 뽑는다. 분류마다 최소 1개가 없으면 이유를 댈 수 있어야 한다.

| 분류 | 무엇 |
|------|------|
| 정상 | 스펙이 규정한 대표 경로 |
| 경계 | 한도·만료·범위의 **양쪽**. `N-1`(통과)과 `N`(실패), 만료 정각과 +1초 |
| 예외 | 규칙 위반 시 던지는 `ErrorType`. 상태코드가 아니라 에러 코드까지 |
| 결정성 | 시각·순서·동시성에 따라 결과가 갈리는 지점 |

```
스펙: 케어 초대는 발송 후 24시간이 지나면 수락할 수 없다.

- 발송 24시간 이내에는 수락할 수 있다              (정상)
- 발송 후 정확히 24시간이면 수락할 수 없다          (경계)
- 발송 후 23시간 59분 59초면 수락할 수 있다         (경계)
- 만료된 초대를 수락하면 CARE_INVITATION_EXPIRED    (예외)
```

기댓값은 **스펙에서 가져온다.** 구현을 실행해 나온 값을 기댓값으로 적으면,
그 테스트는 버그를 정답으로 고정하는 것이다.

---

## 1. 무엇을 테스트할 가치가 있는가

판단 기준은 하나다 — **이 코드가 결정을 내리는가?**
결정 = `if`, 계산, 상태 전이, 반복. 결정이 없으면 틀릴 수 없고, 틀릴 수 없으면 테스트할 게 없다.

```java
// 결정 없음 → 테스트하지 않는다
public Gps findLatest(String wardKey) {
    return gpsHistoryRepository.findLatest(wardKey).orElseThrow();
}

// 결정 있음 → 테스트한다
public boolean isExpired(LocalDateTime now) {
    if (status != PENDING) return true;
    return now.isAfter(createdAt.plusHours(24));
}
```

### 계층별 가치

| 계층 | 가치 | 무엇을 |
|------|------|--------|
| VO / Entity | ★★★ | 상태 전이, 불변식, 생성자 검증. 가장 싸고 가장 많이 |
| Implement | ★★★ | Validator 규칙, Manager 오케스트레이션 순서, 캐시 히트/미스 분기 |
| Business | ★★ | 여러 Implement를 조합하는 시나리오, 예외 변환 |
| Controller | ★ | `@Valid` 검증, 직렬화 포맷, 상태코드. 로직 검증이 아니다 |
| Repository | 조건부 | 직접 쓴 QueryDSL/JPQL만. 파생 메서드 CRUD는 프레임워크 몫 |

**배제:** getter, `equals`/`hashCode`, VO ↔ Entity 단순 매핑, `@NotBlank`가 동작하는지,
JPA가 저장하는지. 내 코드가 아니다.

### 실전 필터

작성 전후로 이 질문을 던진다.

> **이 테스트가 실패하려면 프로덕션 코드의 어느 줄이 어떻게 틀려야 하나?**

답이 구체적이면("24를 48로 바꾸면 깨짐") 좋은 테스트다.
답이 안 나오면 지운다. 커버리지 숫자는 이유가 되지 않는다.

---

## 2. Given-When-Then — When은 한 줄

> 구조·네이밍·Fixture 형식은 `test-conventions.md`에 있다. 여기서는 판단 기준만 다룬다.

**When이 한 줄인 것이 "한 테스트 한 행위"의 실질적 정의다.**

### 단언은 여러 개여도 된다

한 행위의 여러 측면을 단언하는 것은 문제가 아니다. 문제는 **행위를 이어붙이는 것**이다.

```java
// 나쁨 — When이 세 번 나온다
invitation.send();     assertThat(...);
invitation.accept();   assertThat(...);   // 앞에서 실패하면 여기는 실행조차 안 된다
invitation.expire();   assertThat(...);
```

중간에서 실패하면 뒤 단계는 검증되지 않고, 실패 메시지도 어느 단계가 깨졌는지 말해주지 않는다.
`SoftAssertions`는 중단만 해결하고 이름 문제는 해결하지 못한다. 애초에 쪼갠다.

### 이름은 구현이 아니라 규칙을 서술한다

```java
// 나쁨 — 메서드명이 바뀌면 이름이 거짓말이 된다
@DisplayName("isExpired가 status가 PENDING이 아닐 때 true를 반환한다")

// 좋음
@DisplayName("이미 처리된 초대는 만료로 간주한다")
```

`@DisplayName`에 프로덕션 메서드명이 들어간다면 **구현을 보고 테스트를 짰다는 신호다.**

### 테스트 코드에 `if` / `for`를 쓰지 않는다

기댓값을 계산하면 그 계산도 틀릴 수 있고, 실패 시 어느 케이스인지 드러나지 않는다.

```java
@ParameterizedTest
@CsvSource({"23, false", "24, true", "25, true"})
@DisplayName("발송 후 24시간이 지나면 만료된다")
void isExpired_by_elapsed_hours(int hours, boolean expected) {
    assertThat(invitation.isExpired(CREATED_AT.plusHours(hours))).isEqualTo(expected);
}
```

**기댓값은 계산하지 말고 적는다.** 표처럼 읽히고 케이스별로 따로 리포팅된다.

### DAMP > DRY

테스트에서는 중복 제거보다 **혼자 읽히는 것**이 우선이다.
Fixture를 상속으로 엮고 셋업을 여러 단계로 추상화하면 나중에 아무도 고치지 못한다.
Fixture는 상수와 팩토리 메서드까지만. 조금 복붙되는 편이 낫다.

---

## 3. 목으로 바꿀 자격

**하나라도 해당되면 목, 그 외는 실제 객체.**

| 자격 | 이 레포의 예 |
|------|--------------|
| 외부 I/O | Repository, `StringRedisTemplate`, FCM, CoolSMS, `SseEmitter` |
| 비결정성 | 현재 시각, 랜덤, `UUID.randomUUID()` |
| 느림 | 외부 응답 대기, 무거운 배치 |
| 재현 곤란 | 네트워크 타임아웃, `RedisConnectionFailureException` |

```java
// 나쁨 — ObjectMapper는 순수 변환기다. 셋 중 어디에도 해당하지 않는다
@Mock ObjectMapper objectMapper;
given(objectMapper.readValue(json, Gps.class)).willReturn(expected);

// 좋음 — 실제 객체. 직렬화 계약까지 함께 검증된다
@Spy ObjectMapper objectMapper = JsonMapper.builder()
        .addModule(new JavaTimeModule()).build();
```

VO, Entity, Validator, 순수 계산기, `ObjectMapper`는 진짜를 쓴다.
**목은 "실제 협력자가 이렇게 동작할 것"이라는 내 가정을 코드로 굳히는 일이다.**
가정이 틀리면 테스트는 초록불이고 프로덕션만 터진다.

### verify는 호출 자체가 요구사항일 때만

```java
// 정당 — "케어 요청 수락 시 알림을 보낸다"가 요구사항이다
then(notificationSender).should().send(GUARDIAN_KEY, ACCEPTED_TITLE, ACCEPTED_BODY);

// 부당 — 저장은 수단이지 요구사항이 아니다. save를 saveAll로 바꾸면 깨진다
then(gpsHistoryRepository).should().save(any());
```

**상태 검증(반환값·객체 상태)을 먼저 시도하고, 불가능할 때만 행위 검증으로 간다.**
외부로 나가는 부수효과(알림, 이벤트 발행)는 상태로 확인할 방법이 없으니 verify가 맞다.

### 목이 5개를 넘으면 설계 신호다

테스트가 어려운 게 아니라 그 클래스가 너무 많은 것과 얽혀 있는 것이다.
테스트하기 어렵다는 것은 거의 항상 설계 피드백이다.

---

## 4. 시간·랜덤을 주입 가능하게 만든다

```java
// 테스트 불가 — 만료 정각을 재현할 방법이 없다
public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
}
```

이를 피하려고 `LocalDateTime.now().plusDays(1)` 같은 **상대 시각**을 쓰면,
자정 근처 CI나 타임존 차이에서 깨지는 플레이키 테스트가 된다. 경계값은 아예 테스트할 수 없다.

**해법 A — 파라미터로 받는다 (VO / Entity)**

```java
public boolean isExpired(LocalDateTime now) { return now.isAfter(expiresAt); }
```

**해법 B — `Clock`을 주입한다 (Business / Implement)**

```java
private final Clock clock;                          // @Bean Clock.systemDefaultZone()
LocalDateTime now = LocalDateTime.now(clock);

// 테스트
Clock fixed = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
```

랜덤·UUID도 같다. 인터페이스로 감싸 주입하거나 `Random`에 시드를 고정한다.

> **현재 상태:** `src/main`에 무인자 `now()` 호출이 12곳, `Clock` 주입은 0곳이다.
> 기존 코드를 일괄 수정하는 것은 `/feature` 범위다. **신규·수정 코드부터** 위 방식을 적용하고,
> 기존 코드를 테스트할 때 경계값이 막히면 그 사실을 보고한다 — 우회해서 상대 시각을 쓰지 않는다.

**원칙: 제어할 수 없는 것과 결정하는 코드를 분리한다.**
3장과 4장은 같은 이야기다 — 비결정성을 주입 가능하게 만드는 것이 곧 목으로 바꿀 자격을 주는 일이다.
