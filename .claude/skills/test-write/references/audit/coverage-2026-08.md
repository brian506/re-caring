# 테스트 커버리지 측정 결과 (2026-08-26)

JaCoCo 일회성 측정. **`build.gradle`은 변경하지 않았다** (init script 주입 방식, 재현 방법은 문서 하단).

측정 범위: `./gradlew test` — **단위 테스트만.** 통합 테스트(`@Tag("integration")`)는 제외됐다.
제외 클래스: `config/**`, `*Request`, `*Response`, QueryDSL `Q*`, `RecaringApplication`.

---

## 전체 지표

| 지표 | 커버리지 | 절대값 |
|---|---|---|
| Instruction | 61.5% | 6,122 / 9,955 |
| Line | 58.3% | 1,245 / 2,135 |
| Method | 59.6% | 340 / 570 |
| Branch | **55.0%** | 225 / 409 |

Controller 다수가 0%인 것은 통합 테스트 제외 때문이다. 그 112줄을 빼고 계산해도
라인 커버리지는 61.5%로 큰 차이가 없다.

**브랜치 55%** — 409개 분기 중 **184개가 한쪽 경로만** 탔다.
이것이 "반례를 안 썼다"의 직접적 수치이며, 감사에서 발견한 경계값 누락
(한도 `N-1`, 날짜 경계, `updateBattery(false)` 분기 등)과 같은 이야기다.

---

## ⚠️ 이 숫자를 읽는 법

**61.5%는 "검증된 비율"이 아니라 "실행된 비율"이다.** 이 프로젝트는 두 값의 격차가 유난히 크다.

JaCoCo는 줄이 실행됐는지만 센다. 단언의 품질은 보지 않는다. 따라서:

- `GpsLatestCacheManager.find()`는 **커버된 것으로 집계**되지만, 테스트가 `ObjectMapper`를
  mock하기 때문에 정작 깨졌던 직렬화 왕복은 한 번도 실행되지 않았다.
  → SSE 실시간 전송 3일 중단 장애가 이 격차 안에서 발생했다
  (`production-impact-2026-08.md` 참고)
- `CareRelationshipValidator.validateCanAddGuardian`은 테스트가 모든 줄을 실행해
  **커버율이 높게 잡히지만**, 프로덕션에서 절대 성공할 수 없는 코드다
- `GpsHistoryRepositoryCustomImpl.findDailyGpsHistory`는 쿼리가 실행되어 커버로 잡히지만,
  테스트 결과가 항상 빈 리스트라 wardKey 격리를 전혀 검증하지 않는다

**결론: 커버리지 임계값을 CI에 거는 것만으로는 이번에 찾은 문제 중 어느 것도 막히지 않는다.**
위 세 건 전부 "커버된" 상태에서 발생했다.

---

## 커버리지 역전 — 이 측정의 핵심 발견

로직이 없는 곳이 100%, 로직이 있는 곳이 44~58%다.

```
100.0%     20/20     care.business          ← 순수 위임. 동어반복 테스트 6개
100.0%     19/19     safezone.business
100.0%      8/8      sms.business
 96.0%     48/50     notification.business
 89.6%     60/67     auth.business
       ──────────────────────────────────
 57.6%     80/139    care.implement         ← 죽은 validateCanAddGuardian이 여기 있다
 47.8%     11/23     device.implement
 43.9%     29/66     member.implement
 34.5%     10/29     sms.implement
 25.9%     14/54     location.dataaccess
```

`care.business`가 100%인 이유는 `CareInvitationService`가 한 줄짜리 위임 메서드 6개뿐이고,
테스트도 "Manager로 위임했는가"만 확인하는 거울 테스트 6개이기 때문이다.
실행은 100%지만 잡을 수 있는 버그는 사실상 없다.

**따라서 전역 커버리지 임계값은 의미가 없다** — 위임 계층의 100%에 희석되어,
정작 로직이 있는 Implement 계층의 낮은 커버리지를 가린다.
임계값을 건다면 `security.**`, `**.implement.**` 같은 **패키지 단위**로 걸어야 한다.

---

## 0%인 패키지 — 커버리지와 실제 위험이 일치하는 자리

```
   0.0%      0/53     security.filter        ← JwtAuthenticationFilter, DeviceTokenAuthFilter
   0.0%      0/32     security.exception     ← AuthenticationExceptionHandler, JwtAuthenticationEntryPoint
   0.0%      0/31     common.aspect          ← SignAspect
   0.0%      0/10     common.enums
```

**`security.filter` 0%는 통합 테스트 제외 탓이 아니다.** 두 필터는 단위 테스트가 애초에 없다.
모든 요청이 통과하는 인증 관문의 53줄이 한 번도 실행되지 않는다.

여기는 **숫자를 올리는 것이 곧 안전을 올리는** 드문 자리다. 보강 1순위.

아래는 통합 테스트 제외로 인한 0%이므로 실제 위험과 무관하다
(단, 그 통합 테스트들이 CI에서 안 돌아간다는 별도 문제는 있다 — `production-impact-2026-08.md` A-6):

```
   0.0%      0/29     auth.controller
   0.0%      0/29     care.controller
   0.0%      0/26     location.controller
   0.0%      0/14     safezone.controller
   0.0%      0/8      member.controller
   0.0%      0/4      sms.controller
   0.0%      0/2      device.controller
```

---

## 전체 패키지별 라인 커버리지 (낮은 순)

```
   0.0%      0/10     common.enums
   0.0%      0/14     safezone.controller
   0.0%      0/2      device.controller
   0.0%      0/26     location.controller
   0.0%      0/29     auth.controller
   0.0%      0/29     care.controller
   0.0%      0/31     common.aspect
   0.0%      0/32     security.exception
   0.0%      0/4      sms.controller
   0.0%      0/53     security.filter
   0.0%      0/8      member.controller
  25.9%     14/54     location.dataaccess
  34.5%     10/29     sms.implement
  38.0%     38/100    care.dataaccess
  38.9%      7/18     common.utils
  39.5%     51/129    notification.dataaccess
  43.9%     29/66     member.implement
  47.8%     11/23     device.implement
  50.0%      6/12     device.dataaccess
  50.0%      6/12     security.handler
  50.0%     30/60     auth.dataaccess
  57.6%     80/139    care.implement
  59.1%     39/66     member.dataaccess
  60.0%     21/35     safezone.dataaccess
  61.1%     22/36     care.vo
  61.4%    129/210    notification.implement
  63.2%     12/19     notification.controller
  65.6%     21/32     common.controller
  66.7%     28/42     auth.vo
  67.2%     43/64     auth.implement
  75.5%    185/245    location.implement
  76.5%     13/17     member.business
  84.4%     27/32     location.vo
  84.6%     11/13     location.business
  87.0%     60/69     notification.vo
  89.6%     60/67     auth.business
  91.7%     22/24     safezone.vo
  91.9%     91/99     support.exception
  92.0%     23/25     safezone.implement
  93.9%     31/33     security.jwt
  96.0%     48/50     notification.business
 100.0%      1/1      device.business
 100.0%      2/2      care.event
 100.0%      2/2      security.vo
 100.0%      3/3      common.entity
 100.0%      3/3      support.response
 100.0%      4/4      location.event
 100.0%      8/8      sms.business
 100.0%     15/15     sms.vo
 100.0%     19/19     safezone.business
 100.0%     20/20     care.business
```

참고: `security.jwt` 93.9%는 높아 보이지만, 감사에서 확인했듯 access/refresh 토큰 구분,
alg=none, payload 변조 반례가 전부 빠져 있다. 높은 커버리지가 검증 완결성을 뜻하지 않는 또 다른 예다.

---

## 재현 방법

`build.gradle`을 건드리지 않고 측정하려면 init script를 쓴다.

init script 파일 (예: `jacoco-init.gradle`) 내용:

    allprojects {
        apply plugin: 'jacoco'
        afterEvaluate {
            tasks.matching { it.name == 'jacocoTestReport' }.configureEach {
                reports { xml.required = true; html.required = true; csv.required = true }
                classDirectories.setFrom(
                    files(classDirectories.files.collect {
                        fileTree(dir: it, exclude: [
                            '**/Q*.class', '**/RecaringApplication.class',
                            '**/config/**', '**/*Request.class', '**/*Response.class',
                        ])
                    })
                )
            }
        }
    }

실행:

    JAVA_HOME="C:/Users/SSAFY/.jdks/ms-21.0.12" \
      ./gradlew cleanTest test jacocoTestReport --init-script <위 파일 경로>

리포트 위치:
- HTML: `build/reports/jacoco/test/html/index.html`
- CSV:  `build/reports/jacoco/test/jacocoTestReport.csv`

CSV 집계 (전체 + 패키지별):

    CSV=build/reports/jacoco/test/jacocoTestReport.csv
    awk -F, 'NR>1{im+=$4;ic+=$5;bm+=$6;bc+=$7;lm+=$8;lc+=$9;mm+=$12;mc+=$13}
    END{printf "Instruction: %.1f%%\nBranch: %.1f%%\nLine: %.1f%%\nMethod: %.1f%%\n",
    100*ic/(im+ic),100*bc/(bm+bc),100*lc/(lm+lc),100*mc/(mm+mc)}' "$CSV"

통합 테스트까지 포함한 수치를 보려면 Docker를 켜고 `test` 대신
`test integrationTest`를 함께 실행한다. (측정 당시 Docker 미기동으로 확인하지 못함)

---

## 권고

1. **`security.filter` 53줄 보강** — 커버리지와 실제 위험이 일치하는 유일한 자리
2. **JaCoCo 정식 도입 시 임계값은 패키지별로** — 전역 임계값은 위임 계층 100%에 희석된다
3. **커버리지 도구로는 허위 보증을 잡을 수 없다** — `../test-antipatterns.md` 체크리스트를
   코드 리뷰에 병행할 것
