# 오류 분석 가이드

## 빌드/테스트 실패

### 1. 컴파일 에러

증상: `error: cannot find symbol`, `error: incompatible types`

분석 순서:
1. 오류 발생 파일과 라인 번호 확인
2. 해당 파일 Read
3. import 누락, 타입 불일치, 메서드 시그니처 변경 여부 확인
4. QueryDSL Q클래스 누락이면: `./gradlew compileQuerydsl` 실행

### 2. 테스트 실패

증상: `Tests run: N, Failures: N`

분석 순서:
1. `build/reports/tests/test/` 리포트 확인
2. 실패한 테스트 클래스/메서드 특정
3. 테스트 코드와 실제 구현 비교
4. 아키텍처 규칙 위반 여부 확인 (CLAUDE.md 참고)

### 3. 자주 발생하는 패턴

| 오류 | 원인 | 해결 |
|------|------|------|
| `NoSuchBeanDefinitionException` | Bean 등록 누락 | `@Component`, `@Service` 확인 |
| `DataIntegrityViolationException` | DB 제약조건 위반 | Entity 필드 `nullable`, `unique` 확인 |
| `AppException(ErrorType.xxx)` | 비즈니스 규칙 위반 | ErrorType 정의 확인 |
| QueryDSL Q클래스 없음 | APT 미실행 | `./gradlew compileQuerydsl` |

---

## CI 실패

### 로그 확인

```bash
gh run list --repo brian506/re-caring --limit 5
gh run view {run-id} --repo brian506/re-caring --log-failed
```

### 자주 발생하는 CI 실패 패턴

| 증상 | 원인 | 해결 |
|------|------|------|
| `./gradlew: Permission denied` | gradlew 실행 권한 없음 | `chmod +x gradlew` 커밋 |
| 테스트 컨테이너 연결 실패 | Testcontainers Docker 이슈 | CI 환경 docker 설정 확인 |
| 빌드 캐시 문제 | Gradle 캐시 오염 | `./gradlew clean` 후 재시도 |
| 환경변수 누락 | GitHub Actions secrets 미설정 | `.github/workflows/ci-check.yml` env 섹션 확인 |

### CI 재시도 프로세스

1. 실패 로그에서 원인 파악
2. 로컬에서 `./gradlew clean build` 재검증
3. 수정 커밋 후 push (CI 자동 재트리거):
   ```bash
   bash scripts/commit-and-push.sh {N} "CI 오류 수정 - {원인 요약}"
   ```
4. `bash scripts/wait-for-ci.sh {PR번호}` 재실행

---

## 배포 실패

### 로그 확인

```bash
gh run view {run-id} --repo brian506/re-caring --log-failed
```

### 자주 발생하는 배포 실패 패턴

| 증상 | 원인 | 해결 |
|------|------|------|
| Docker 이미지 push 실패 | Docker Hub 인증 만료 | GitHub secrets 갱신 필요 (수동) |
| SSH 연결 실패 | 서버 상태 이상 | 서버 직접 확인 필요 (수동) |
| AWS Parameter Store 조회 실패 | IAM 권한 만료 | AWS 설정 확인 필요 (수동) |
| 헬스체크 실패 | 앱 기동 실패 | 서버 로그 확인 필요 (수동) |

> 배포 실패는 인프라 레벨 이슈가 많아 자동 수정이 어렵다. 원인 분석 후 사용자에게 보고한다.
