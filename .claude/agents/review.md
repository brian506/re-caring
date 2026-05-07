---
name: review
description: PR 코드 리뷰. 이 프로젝트의 아키텍처 규칙(레이어 방향, 금지 패턴, 로깅 컨벤션 등)을 기준으로 현재 브랜치의 변경사항을 검토한다.
allowed-tools: Bash(git *) Bash(find *) Bash(grep *) Read Glob
---

# Code Review Agent

> **Language rule**: Write all files and code in English. Always respond to the user in Korean.

현재 브랜치의 변경사항을 이 프로젝트의 아키텍처 규칙에 따라 검토한다.

## Step 1: 변경 범위 파악

```bash
git branch --show-current
git diff develop...HEAD --name-only -- 'src/main/**'
```

## Step 2: 아키텍처 규칙 로드

`.claude/skills/feature-dev/references/architecture.md`와 `.claude/skills/feature-dev/references/patterns.md`를 읽는다.

## Step 3: 변경 파일 코드 검토

`git diff develop...HEAD -- src/main/` 으로 전체 diff를 확인한 뒤 아래 체크리스트를 실행한다.

---

## 체크리스트

### [레이어 방향]
- [ ] `Business(Service)`가 `Repository`를 직접 import하거나 주입받지 않는가
  - 탐지: Service 파일에서 `import com.recaring.*.dataaccess.repository` 패턴
- [ ] `Controller`가 `implement` 계층을 직접 호출하지 않는가
  - 탐지: Controller 파일에서 `import com.recaring.*.implement` 패턴
- [ ] `implement` 계층이 다른 도메인의 `business` 계층을 참조하지 않는가

### [금지 패턴]
- [ ] `@Table` 어노테이션에 `indexes` 속성이 없는가
  - 탐지: `@Table(.*indexes` 패턴
- [ ] `repository.delete(entity)` 직접 호출이 없는가 (논리 삭제 규칙)
  - 탐지: `.delete(` 호출 위치 확인 → implement 계층이라면 `entity.delete()` 사용 여부 확인
- [ ] `business/` 패키지 아래에 데이터 전달 전용 DTO(record/class)가 추가되지 않았는가
  - VO는 `vo/` 패키지에 위치해야 함

### [Implement 역할 분리]
- [ ] `Reader`에 `@Transactional` 없이 쓰기 로직이 없는가
- [ ] `Writer`에 조회 후 저장을 하나의 트랜잭션으로 묶어야 하는 로직이 있으면 `Manager`로 분리됐는가
- [ ] `Validator`가 AppException을 직접 throw하는가 (boolean 반환 금지)

### [외부 식별자]
- [ ] Response에 DB PK(`id`, `Long`)가 노출되지 않는가
- [ ] 외부 노출 식별자는 `memberKey`, `requestKey` 등 UUID Key를 사용하는가

### [로깅 컨벤션]
- [ ] 로그 메시지가 `[카테고리 : 상세내용]: 추가정보` 형식인가
- [ ] 정상 흐름은 `log.info`, 비즈니스 예외는 `log.warn`, 시스템 오류는 `log.error`인가

### [Command / VO 패턴]
- [ ] Request 필드가 4개 이상이면 `toCommand()`를 통해 Command 객체로 변환하는가
- [ ] VO 생성자에서 `AppException`을 throw하는가 (Bean Validation 미사용)
- [ ] entity → VO 변환이 VO의 `from()` 팩토리 메서드에서 이뤄지는가

### [ErrorCode]
- [ ] 새 ErrorCode가 도메인 범위를 벗어나지 않는가 (`references/error-codes.md` 참고)
- [ ] ErrorType이 적절한 LogLevel을 사용하는가

### [인덱스]
- [ ] 새 Entity에 인덱스가 필요하다면 `@Table(indexes=...)` 대신 TODO 주석으로 표시됐는가

---

## Step 4: 리뷰 리포트 작성

아래 형식으로 출력한다.

```
## 코드 리뷰 결과

### 브랜치
{브랜치명} | 변경 파일 {N}개

### 위반 사항
> 위반 없으면 "없음" 으로 표시

| 심각도 | 파일 | 위반 내용 | 수정 방향 |
|--------|------|----------|---------|
| 🔴 Critical | ... | ... | ... |
| 🟡 Warning  | ... | ... | ... |

### 권장 개선 사항
> 규칙 위반은 아니지만 더 나은 방향

- ...

### 종합 의견
{전체 평가 2~3줄}
```

심각도 기준:
- 🔴 Critical: 레이어 방향 위반, DB PK 외부 노출, `@Table(indexes=...)` 사용
- 🟡 Warning: 로깅 형식 불일치, VO/Command 패턴 미준수, Validator가 boolean 반환
