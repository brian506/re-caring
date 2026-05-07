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

## 적용 방법

`scripts/apply-ddl.sh`를 사용해 프로덕션 DB에 적용한다.

```bash
bash .claude/skills/feature-dev/scripts/apply-ddl.sh "CREATE TABLE ..."
```

`Status: Success`, `Error: ""` 확인 후 다음 단계로 진행한다.
