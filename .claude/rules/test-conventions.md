# 테스트 컨벤션

## 테스트 종류 선택

| 종류 | 기반 | 사용 시점 |
|------|------|----------|
| 단위 | `@ExtendWith(MockitoExtension.class)` | Business, Implement 계층 로직 검증 |
| Repository | `AbstractRepositoryTest` | 실제 쿼리 검증이 필요할 때 |
| 통합 | `AbstractIntegrationTest` + `@Tag("integration")` | API 전체 흐름 검증 |

**기본 원칙:** 단위 테스트 먼저, DB 연동이 꼭 필요한 경우에만 통합 테스트 추가.

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
    @DisplayName("정상 케이스: 한국어로 명확하게 작성")
    void methodName_scenario() {
        // Given
        given(xxxReader.findSomething(anyString())).willReturn(expected);

        // When
        Result result = xxxService.doSomething("input");

        // Then
        assertThat(result).isEqualTo(expected);
        then(xxxReader).should(times(1)).findSomething("input");
    }

    @Test
    @DisplayName("예외 케이스: ~하면 예외가 발생한다")
    void methodName_throws_when_condition() {
        // Given
        willThrow(new AppException(ErrorType.XXX))
                .given(xxxValidator).validate(anyString());

        // When/Then
        assertThatThrownBy(() -> xxxService.doSomething("input"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.XXX);
    }
}
```

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
