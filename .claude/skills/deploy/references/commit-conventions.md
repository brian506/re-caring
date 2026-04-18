# 커밋 메시지 컨벤션

## 형식

```text
{type}[#{이슈번호}]: {설명}
```

## Type 목록

| Type | 설명 |
|------|------|
| `feature` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `style` | 코드 포맷 변경 (로직 수정 없음) |
| `docs` | 문서 작성/수정 |
| `test` | 테스트 코드 작성/변경 |
| `hotfix` | 긴급 수정 (치명적 버그) |
| `chore` | 빌드/패키지 구성 업데이트 |

## 예시

```text
feature[#23]: 회원 프로필 조회 API 구현
fix[#34]: 로그인 토큰 만료 처리 버그 수정
refactor[#41]: LocalAuthManager 책임 분리
```

## CI 재시도 커밋 메시지

CI 실패로 인한 수정 커밋은 `fix` 타입을 사용한다:

```text
fix[#23]: CI 오류 수정 - QueryDSL Q클래스 생성 누락
fix[#23]: CI 오류 수정 - 테스트 환경 변수 누락
```
