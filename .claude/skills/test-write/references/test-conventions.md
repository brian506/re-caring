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
