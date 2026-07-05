# DDL Conventions

Hibernate가 `spring.jpa.hibernate.ddl-auto=validate` 모드이므로, **코드 배포 전에 반드시** 프로덕션 DB에 스키마를 적용해야 한다.
순서가 바뀌면 앱이 기동 불가 상태(crash loop)에 빠진다.

## 엔티티 → DDL 변환 규칙

| 엔티티 어노테이션 | DDL |
|---|---|
| 컬럼명 (`camelCase`) | `snake_case` (Spring 기본 네이밍 전략) |
| `@GeneratedValue(strategy = IDENTITY)` | `BIGSERIAL PRIMARY KEY` |
| `nullable = false` | `NOT NULL` |
| `unique = true` | `UNIQUE` |
| `@CreatedDate` | `TIMESTAMP NOT NULL` |
| `@Column` (선택) | `TIMESTAMP` (nullable) |

- `ddl-auto=create`에 의존하지 않고 DDL을 직접 작성한다.
- 인덱스는 DDL에 포함하지 않고 엔티티 파일에 TODO 주석으로만 남긴다 (CLAUDE.md 인덱스 규칙).

## 워크플로: pending ledger 방식

기능마다 완결된 DDL 파일을 손으로 만들지 않는다. 대신 **브랜치용 단일 ledger 파일** `docs/pending-ddl.sql`에 변경분을 누적하고, 배포 시점에 한 번 적용한 뒤 삭제한다.

1. **구현 중 (`/feature`)**: 엔티티가 바뀔 때마다 변경분 DDL을 `docs/pending-ddl.sql`에 **append**만 한다. DB에 적용하지 않는다. (로컬은 `ddl-auto=create`라 불필요)
   - 이 파일은 gitignore됨 → 커밋·PR·develop에 남지 않는 로컬 전용 이력.
2. **배포 시 (`/deploy`)**: CI 통과 후 **머지 직전**에 `docs/pending-ddl.sql`을 읽어 dev DB에 적용한다.
   - `bash .claude/skills/deploy/scripts/apply-pending-ddl.sh docs/pending-ddl.sql`
   - `Status: Success` 확인 후 파일을 삭제하고 나서 머지·배포한다.
   - 파일이 없거나 비어 있으면 이번 배포에 스키마 변경이 없는 것 → 스킵.

> 순서 보장: 머지 → GitHub Actions가 즉시 ECS 배포 → 앱이 `ddl-auto=validate`로 기동.
> 따라서 스키마 적용은 반드시 **머지 이전**에 끝나 있어야 crash loop를 피한다.

### ledger 작성 시 반드시 포함

엔티티에서 실제 바뀐 것을 정확히 반영한다. 과거에 누락으로 장애가 났던 항목:
- **삭제된 테이블의 FK 의존성** — 참조하는 테이블부터 먼저 `DROP` (아니면 `DROP TABLE`이 실패)
- **삭제된 컬럼** — 엔티티에서 지웠는데 DB에 `NOT NULL`로 남으면 신규 INSERT가 런타임에 실패
