# 테스트 컨벤션

## 테스트 종류 선택

| 종류 | 기반 | 사용 시점 |
|------|------|----------|
| 단위 | `@ExtendWith(MockitoExtension.class)` | VO, Implement, Business 계층 로직 검증 |
| 통합 | `AbstractIntegrationTest` | Controller — API 전체 흐름. 실제 DB에 붙어 부수효과까지 확인 |

`AbstractIntegrationTest`가 `@Tag("integration")`을 상속시키므로 하위 클래스에 따로 붙이지 않아도 된다.

**Repository·Entity는 별도 테스트 파일을 만들지 않는다.** 커스텀 QueryDSL 쿼리도 마찬가지다 —
쿼리는 Controller 통합 테스트가 실제로 통과시키고, Entity 상태 전이는 그 Entity를 다루는 Implement 테스트로 간접 검증한다.

**Controller는 Mock 기반으로 쓰지 않는다.** `@WebMvcTest`·`MockMvc` + `@MockBean` 금지.

**기본 원칙:** 단위 테스트 먼저, Controller만 통합 테스트.

## VO 단위 테스트는 "계산·파싱"만 남긴다

> 테스트의 목적은 **비즈니스 로직의 중대한 오류를 막는 것**이다.
> 순수 코드가 잘 도는지 확인하는 테스트는 쓰지 않는다.

`vo/`에 있다는 이유로도, 생성자에서 `AppException`을 던진다는 이유로도 테스트를 쓰지 않는다.
가르는 기준은 하나다 — **틀릴 수 있는 방법이 여러 개인가.**

| 쓴다 | 안 쓴다 |
|------|---------|
| haversine 거리 판정 (`SafeZoneInfo.contains()`) | enum 비교 한 번 (`if (role != GUARDIAN) throw`) |
| 파싱·직렬화 왕복 (`BatteryThresholds.parse()/format()`) | null/blank 가드 하나 (`EncodedPassword`) |
| 정규식·길이 경계 (`PhoneNumber`, `Password`, `LocalEmail`, `SmsCode`) | 단순 삼항 판정 (`Gps.occurredAt()`, `isAccurate()`) |
| 복합 조건 범위 (`BatteryThreshold` — 범위 + 10단위 배수) | `from(Entity)` 매핑, getter 왕복, enum 상수 나열 |

**왼쪽은 정답이 하나가 아니다.** 정규식은 수십 가지로 틀릴 수 있고, haversine은 공식·단위·부호로
틀릴 수 있으며, "반경 정확히 경계값"·"비밀번호 정확히 20자" 같은 경계는 통합 테스트로 재현할
방법이 사실상 없다. 목도 컨테이너도 없어 실행 비용은 0에 가깝다.

**오른쪽은 정답이 하나뿐이고, 그 하나는 흐름 테스트가 이미 통과시킨다.**
게다가 VO 단위 테스트는 그 검증이 **실제로 호출되는지를 증명하지 못한다.** 진짜 결함은
`if`문이 틀리는 게 아니라 아무도 그 VO를 안 거치고 흐름이 지나가는 것이다.
`Caregiver.of()`가 완벽히 맞고 완벽히 테스트돼 있어도 Manager가 호출을 빠뜨리면
테스트는 전부 초록불이고 프로덕션만 뚫린다. 그래서 **잘못된 값의 거부는 그 값을 쓰는
Implement·Business·Controller 테스트에서** 확인한다.

같은 기준을 Implement·Business에도 적용한다. `return reader.findX(key)` 한 줄짜리 위임
메서드는 스텁이 준 값을 그대로 되받을 뿐이라 테스트하지 않는다.

## 파일 위치

| 대상 | 위치 |
|------|------|
| VO | `src/test/java/com/recaring/{domain}/vo/` |
| Implement (`*Reader`, `*Writer`, `*Manager`, `*Validator`) | `src/test/java/com/recaring/{domain}/implement/` |
| Business (`*Service`) | `src/test/java/com/recaring/{domain}/business/` |
| Controller (통합) | `src/test/java/com/recaring/{domain}/controller/` |
| Fixture | `src/test/java/com/recaring/{domain}/fixture/` |

## 기댓값은 구현에서 역산하지 않는다

기댓값은 이슈·요구사항·API 명세에서 가져온다.
구현을 실행해 나온 값을 기댓값으로 적으면 버그를 정답으로 고정하게 된다.
판단이 서지 않으면 이 질문을 던진다 — **이 테스트가 실패하려면 프로덕션의 어느 줄이 어떻게 틀려야 하나?**
답이 안 나오면 그 테스트는 지운다.

## 단위 테스트 구조

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("XxxService 단위 테스트")
class XxxServiceTest {

    @InjectMocks
    private XxxService xxxService;

    @Mock
    private XxxReader xxxReader;

    @Test
    @DisplayName("만료된 항목은 결과에서 제외한다")
    void doSomething_excludes_expired_items() {
        // Given — 인자는 값으로 고정한다. any() 계열로 흘리지 않는다
        given(xxxReader.findSomething(MEMBER_KEY)).willReturn(List.of(ACTIVE, EXPIRED));

        // When — 검증 대상 행위 하나
        List<Item> result = xxxService.doSomething(MEMBER_KEY);

        // Then
        assertThat(result).containsExactly(ACTIVE);
    }

    @Test
    @DisplayName("이미 처리된 요청이면 예외가 발생한다")
    void doSomething_throws_when_already_handled() {
        // Given
        willThrow(new AppException(ErrorType.XXX))
                .given(xxxValidator).validate(MEMBER_KEY);

        // When/Then — 에러 타입까지 단언한다
        assertThatThrownBy(() -> xxxService.doSomething(MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.XXX);
    }
}
```

- `@DisplayName`은 **요구사항 문장**으로 쓴다. 프로덕션 메서드명을 넣지 않는다
- 스텁한 호출을 그대로 `verify` 하지 않는다 (`given` 후 `then(...).should()`)
- 스텁이 준 값을 그대로 비교하는 단언은 아무것도 검증하지 않는다

## Fixture 클래스

```java
public class XxxFixture {
    // 상수 — 테스트 전체에서 재사용
    public static final String MEMBER_KEY = "test-member-key";
    public static final String PHONE = "01011112222";

    // 팩토리 메서드 — 엔티티 생성
    public static Member createMember() {
        return Member.builder()
                .phone(PHONE)
                .name("테스트유저")
                .role(MemberRole.GUARDIAN)
                .build();
    }
}
```

- `src/test/java/com/recaring/{domain}/fixture/` 패키지에 위치
- 상수는 `public static final`
- 팩토리 메서드는 `public static`

## 통합 테스트 (필요한 경우만)

```java
@Tag("integration")
class XxxIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("API 통합 테스트: ~시 ~을 반환한다")
    void api_integration_test() {
        // RestTestClient 사용
    }
}
```

- 반드시 `@Tag("integration")` 추가 (단위 테스트와 분리 실행)
- `./gradlew integrationTest` 로만 실행됨
