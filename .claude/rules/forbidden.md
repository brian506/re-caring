# Forbidden Patterns

절대 위반하면 안 되는 규칙. 코드 리뷰에서 매번 체크된다.

## Architecture

- **메서드 파라미터에 JPA 엔티티 객체를 전달하면 안 된다**
  - 엔티티는 `dataaccess` 계층 내부에서만 사용하며, 계층 간 또는 클래스 간 데이터 전달에 사용하면 안 됨
  - 계층 간 전달은 VO(`vo/` 패키지의 record)나 원시 타입(String, Long 등)을 사용
  - 잘못된 예: `register(CareInvitation invitation, String memberKey)`, `accept(CareInvitation request)`
  - 올바른 예: `register(CareRelationshipRegistration registration, String memberKey)`, `accept(String requestKey)`
  - 예외: VO의 `from(Entity entity)` / `of(Entity entity)` 팩토리 메서드는 entity → VO 변환 전용이므로 허용

- **Business layer는 Repository를 직접 import/주입받으면 안 된다**
  - Service는 반드시 Implement 계층(Reader, Writer, Manager 등)만 참조
  - 탐지: Service 파일에서 `import com.recaring.*.dataaccess.repository` 패턴

- **Controller는 Implement 계층을 직접 호출하면 안 된다**
  - 탐지: Controller 파일에서 `import com.recaring.*.implement` 패턴

- **Implement 계층이 다른 도메인의 Business 계층을 참조하면 안 된다**

- **`business/` 패키지 아래에 데이터 전달 전용 DTO(record/class)를 두면 안 된다**
  - VO는 반드시 `vo/` 패키지에 위치

## Entity & Database

- **`@Table(indexes = ...)` 어노테이션을 사용하면 안 된다**
  - 인덱스는 별도 DDL 스크립트로 관리, 엔티티에 TODO 주석으로 표시
  - 올바른 예: `// TODO: CREATE INDEX idx_xxx ON yyy(zzz);`

- **삭제는 모두 hard-delete로 처리한다 (soft-delete 금지)**
  - `@SQLDelete` / `@SQLRestriction` / `deleted_at` 컬럼을 새로 도입하면 안 된다
  - 실제 삭제: `repository.delete(entity)` 또는 파생 `deleteBy...` / `@Modifying` 벌크 쿼리 사용

- **스키마 변경 적용 전에 코드를 머지/배포하면 안 된다**
  - `spring.jpa.hibernate.ddl-auto=validate` — 스키마 불일치 시 시작 즉시 충돌(crash loop)
  - 스키마 변경은 기능마다 별도 DDL 파일을 만들지 않고, 구현 중 `docs/pending-ddl.sql`(gitignore된 브랜치용 ledger)에 변경분을 append한다.
  - `/deploy`가 CI 통과 후·머지 직전에 `apply-pending-ddl.sh`로 dev DB에 적용하고 `Status: Success` 확인 후 파일을 삭제한다. 이 확인 전까지 머지 금지.
  - 자세한 흐름 → `.claude/rules/ddl-conventions.md`

## Swagger / API Documentation

- **Controller 메서드에 `@ApiResponses`를 사용하면 안 된다**
  - 에러 응답은 `ErrorType`에 집중 관리하며, Swagger에 개별 응답 코드를 나열하지 않는다
  - `@Operation(summary, description)`만 허용
  - 탐지: Controller 파일에서 `@ApiResponses` 패턴

## Code Structure

- **클래스 내부에 중첩 타입을 선언하면 안 된다**
  - 클래스, 레코드, 열거형, 인터페이스는 모두 별도 파일로 분리

- **API Response에 DB PK(`id`, `Long`)를 노출하면 안 된다**
  - 외부 노출 식별자는 `memberKey`, `requestKey` 등 UUID Key 사용

## Testing

- **동일 도메인에 기존 Fixture 클래스가 있는데 새 파일을 만들면 안 된다**
  - 반드시 기존 `*Fixture` 클래스에 메서드를 추가

- **테스트를 통과시키기 위해 `src/main/` 코드를 수정하면 안 된다**
  - 구현 버그라면 `/feature` 스킬로 수정

- **Business 계층 테스트에서 Repository를 직접 Mock하면 안 된다**
  - Implement 클래스(Reader, Writer 등)를 Mock해야 레이어 계약 유지

- **DB 연동 테스트에 `@Tag("integration")`을 누락하면 안 된다**
  - 누락 시 DB 연동 테스트가 단위 테스트 suite에 섞여 실행 시간 급증

## Infrastructure

- **EC2 접근에 SSH(포트 22)를 사용하면 안 된다**
  - AWS SSM Session Manager만 사용

- **인스턴스 ID를 하드코딩하면 안 된다**
  - 태그로 동적 조회: `Name=recaring-app-server`
