# Git Hooks

`.git/hooks/`는 git이 추적하지 않는다. 원본은 여기에 두고 심볼릭 링크가 아닌 복사로 설치한다.

## 설치

```bash
cp .claude/hooks/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

## 제거

```bash
rm .git/hooks/pre-commit
```

## pre-commit — 결정적 검사 4종

AI를 쓰지 않는다. grep·awk만 쓰므로 즉시 끝나고, 판정이 매번 같다.
AI 판단이 필요한 검토는 `test-review` 에이전트가 맡는다 (`test-write` 스킬 Step 5).

| 검사 | 수준 | 근거 |
|------|------|------|
| `@Mock ObjectMapper` | **차단** | 직렬화 왕복이 검증되지 않는다. 실제 장애 이력 있음 |
| `AbstractRepositoryTest` 상속 + `@Tag("integration")` 누락 | **차단** | DB 테스트가 단위 suite에 섞인다 |
| `doesNotThrowAnyException`이 유일한 단언 | 경고 | 빈 메서드로도 통과한다 |
| 무인자 `now()` 신규 추가 | 경고 | 경계값 테스트가 불가능해진다 |

경고는 커밋을 막지 않는다. 차단 항목만 `exit 1`이다.

## 설치 시점의 기존 위반 (2026-08-26)

새로 스테이징하는 파일에만 걸리므로 아래는 커밋을 막지 않는다. 리팩토링 대상 목록이다.

```
@Mock ObjectMapper
  location/implement/gps/GpsLatestCacheManagerTest.java:35

doesNotThrowAnyException 단독
  auth/implement/oauth/OAuthLinkValidatorTest.java
  care/implement/CareRelationshipValidatorTest.java
  care/implement/CareRelationshipWriterTest.java

무인자 now()   src/main 전체 12곳 / Clock 주입 0곳
```

## 우회

`git commit --no-verify`로 건너뛸 수 있다.
막을 방법은 없으므로, 차단 항목은 **우회할 이유가 없을 만큼 명확한 것만** 넣는다.
판단이 갈리는 항목을 차단으로 올리면 `--no-verify`가 습관이 되어 훅 전체가 무력화된다.
