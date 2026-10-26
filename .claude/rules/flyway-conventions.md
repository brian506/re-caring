# Flyway Conventions

스키마는 Flyway 마이그레이션(`src/main/resources/db/migration/V{n}__{description}.sql`)으로만 바꾼다.
앱이 뜰 때 Flyway가 미적용 버전을 실행하고, 이어서 Hibernate가 `ddl-auto=validate`로 엔티티와 대조한다.
local·test도 같은 마이그레이션으로 스키마를 만들므로, SQL과 엔티티가 어긋나면 Testcontainers 통합 테스트에서 먼저 깨진다.

## 프로파일별 동작

| 프로파일 | Flyway | 비고 |
|---|---|---|
| local | 적용 | 빈 DB 필요. 기존 볼륨이 Hibernate `create`로 만든 스키마라면 `docker compose -f docker-compose-local.yml down -v` 후 재기동 |
| test | 적용 | Testcontainers `postgres:17-alpine` |
| dev | 적용 | 기존 스키마를 `baseline-version: 1`로 인정. `V1__baseline.sql`은 dev에서 실행되지 않는다 |
| prod | 비활성 (`enabled: false`) | 전환 시 dev와 같은 baseline 절차를 밟는다. **전환 전까지는 V2 이상을 prod 배포 전에 수동으로 먼저 적용해야 한다** (안 그러면 `validate`로 crash loop) |

## 파일 작성 규칙

- 모든 마이그레이션은 첫 줄에 `SET LOCAL lock_timeout = '5s';`를 둔다. 락을 못 잡으면 오래 막히는 대신 실패하고 앱이 기동하지 않는다. `SET`(세션 범위)은 쓰지 않는다 — Flyway는 앱 Hikari 풀 커넥션을 쓰므로 세션 값이 런타임 쿼리에 남는다.
- 버전은 기존 최댓값 + 1. 다른 브랜치와 번호가 겹치면 머지 전에 뒤쪽 브랜치가 번호를 올린다.
- **이미 적용된 파일은 절대 수정·삭제하지 않는다.** checksum이 달라져 validate가 실패하고 앱이 뜨지 않는다. 고칠 게 있으면 새 버전을 추가한다.
- 엔티티에서 실제 바뀐 것을 정확히 반영한다. 과거 누락 장애 항목:
  - **삭제된 테이블의 FK 의존성** — 참조하는 테이블부터 먼저 `DROP`
  - **삭제된 컬럼** — 엔티티에서 지웠는데 DB에 `NOT NULL`로 남으면 신규 INSERT가 런타임에 실패
- 인덱스도 마이그레이션에 넣는다. 큰 테이블은 `CREATE INDEX CONCURRENTLY`를 쓰고, 그 파일만 트랜잭션 밖에서 돌도록 같은 이름의 설정 파일 `V{n}__{description}.sql.conf`에 `executeInTransaction=false`를 둔다. 이 파일에는 다른 문장을 섞지 않는다. 트랜잭션 밖이라 `SET LOCAL`이 효과가 없으므로, 이 경우에만 `SET lock_timeout = '5s';`를 쓰고 마지막 줄에 `RESET lock_timeout;`을 둔다.

## 엔티티 → DDL 변환 규칙

| 엔티티 어노테이션 | DDL |
|---|---|
| 컬럼명 (`camelCase`) | `snake_case` (Spring 기본 네이밍 전략) |
| `@GeneratedValue(strategy = IDENTITY)` | `BIGSERIAL PRIMARY KEY` |
| `nullable = false` | `NOT NULL` |
| `unique = true` | `UNIQUE` |
| `@CreatedDate` | `TIMESTAMP NOT NULL` |
| `@Column` (선택) | `TIMESTAMP` (nullable) |

## 무중단 배포 제약 (expand → contract)

ECS 롤링 배포 중에는 새 스키마 위에서 **이전 버전 태스크가 잠시 함께 돈다.** 이전 코드가 깨지는 변경은 한 번에 하지 않는다.

- 컬럼 삭제: ① 코드에서 사용 제거 배포 → ② 다음 배포에서 `DROP COLUMN`
- 컬럼 이름 변경: ① 새 컬럼 추가 + 양쪽 쓰기 → ② 읽기 전환 → ③ 이전 컬럼 삭제
- `NOT NULL` 추가: 기본값·백필을 먼저 하고 제약은 다음 버전에서

## 이상탐지 엔진 영향 테이블

엔진(`recaring-detection`)이 `safe_zones`·`gps_histories`를 직접 SQL로 읽는다. 이 두 테이블의 컬럼을 바꾸거나 지우는 마이그레이션은:

1. 엔진 쪽 대응을 먼저 배포한다. 엔진 태스크 정의는 `recaring-detection-detect:latest`를 쓰므로 새 이미지를 푸시한 뒤 `recaring-detection-service`를 강제 재배포해야 반영된다.
2. 머지 직전 dev DB `pg_stat_activity`에 `idle in transaction` 세션이 없는지 확인한다 (`/deploy` Step 4.5).

`gps_histories`는 `recorded_at` 월별 RANGE 파티션 테이블이다. 컬럼 변경은 부모 테이블에 걸면 파티션에 전파된다.
