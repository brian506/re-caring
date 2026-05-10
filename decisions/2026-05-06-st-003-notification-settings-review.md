# 2026-05-06 ST-003 알림 설정 PM 리뷰 노트

## Context

- 대상 화면: `ST-003` 알림 설정, `ST-003-1` 안심존 알림 설정, `ST-003-2` 이상탐지 알림 설정, `ST-003-3` 응급호출 알림 설정, `ST-003-4` 배터리 잔량 알림 설정.
- 현재 명세 위치: `docs/기능명세서.md`의 ST-003 ~ ST-003-4.
- 현재 더보기 API는 `NOTIFICATION_SETTING` 메뉴 항목의 화면 타겟으로 `ST-003`을 내려준다.
- 이번 백엔드 범위는 알림 설정 저장/조회 API까지이며, 실제 푸시 발송 조건 연동은 제외한다.

## Decisions

1. `ST-003` 알림 카테고리 목록 화면은 정적 화면으로 처리한다.
2. `ST-003-1 ~ ST-003-4` 세부 설정 화면은 사용자 입력값을 저장해야 하므로 API를 제공한다.
3. 알림 설정은 대상자별 공통 설정으로 저장한다.
4. 대상자별 공통 설정은 `wardKey` 하나에 설정 row 하나가 있다는 뜻이다.
5. 주보호자, 관계자, 대상자 본인은 같은 대상자의 동일한 알림 설정값을 조회한다.
6. 주보호자, 관계자, 대상자 본인 모두 설정 수정이 가능하다.
7. 보호 관계가 없는 사용자는 해당 대상자의 설정 조회/수정이 불가능하다.
8. 토글, 민감도 슬라이더, 배터리 임계값 선택은 즉시 저장 방식으로 처리한다.
9. 이상탐지 민감도는 5단계로 저장한다.
10. 배터리 임계값은 단일 선택으로 저장한다.
11. 접근금지구역 알림 설정은 이번 범위에서 제외한다.
12. 이번 구현은 설정 저장/조회까지만 포함하고, 실제 알림 발송 필터링은 후속 작업으로 분리한다.

## Rationale

- `ST-003` 목록은 역할, 대상자, 저장 상태에 따라 달라지는 값이 없으므로 서버 API 없이 앱에서 정적으로 렌더링해도 된다.
- 세부 설정 화면은 사용자가 변경한 토글/슬라이더/임계값을 앱 재실행 후에도 유지해야 하므로 서버 저장소가 필요하다.
- 대상자별 공통 설정은 개인별 설정보다 MVP 구현이 단순하고, "주보호자/관계자/대상자 모두 공통"이라는 정책과 맞다.
- GET API는 하나로 두는 것을 권장한다. 네 개 세부 화면의 설정을 한 번에 내려주면 화면 이동 시 추가 왕복 요청을 줄이고, 앱이 동일한 설정 스냅샷을 기준으로 렌더링할 수 있다.
- PATCH API는 카테고리별로 분리하는 것을 권장한다. 즉시 저장 UI에서는 변경된 영역만 작게 보내는 편이 실패 처리와 테스트가 단순하다.

## Proposed API

### 전체 설정 조회

```text
GET /api/v1/notifications/settings/{wardKey}
```

- 목적: ST-003-1 ~ ST-003-4에서 필요한 현재 설정값과 선택 옵션을 한 번에 조회한다.
- 인증: JWT `@AuthMember`.
- 접근: 대상자 본인, 해당 대상자의 주보호자, 해당 대상자의 관계자.
- 응답: `ApiResponse<NotificationSettingResponse>`.

### 안심존 알림 설정 수정

```text
PATCH /api/v1/notifications/settings/{wardKey}/safe-zone
```

- 요청값: 안심존 진입 알림 여부, 안심존 이탈 알림 여부.
- 성공 응답: `ApiResponse<Void>`.

### 이상탐지 알림 설정 수정

```text
PATCH /api/v1/notifications/settings/{wardKey}/anomaly
```

- 요청값: 경로 이탈 알림 여부, 속도 이상 알림 여부, 배회 이상 알림 여부, 5단계 민감도.
- 성공 응답: `ApiResponse<Void>`.

### 응급호출 알림 설정 수정

```text
PATCH /api/v1/notifications/settings/{wardKey}/emergency-call
```

- 요청값: 응급호출 알림 여부.
- 성공 응답: `ApiResponse<Void>`.

### 배터리 잔량 알림 설정 수정

```text
PATCH /api/v1/notifications/settings/{wardKey}/battery
```

- 요청값: 배터리 부족 알림 여부, 배터리 임계값.
- 성공 응답: `ApiResponse<Void>`.

## Proposed Response Shape

```json
{
  "safeZone": {
    "entryEnabled": true,
    "exitEnabled": true
  },
  "anomaly": {
    "routeDeviationEnabled": true,
    "speedAnomalyEnabled": true,
    "wanderingAnomalyEnabled": true,
    "sensitivity": "NORMAL",
    "sensitivityOptions": ["VERY_LOW", "LOW", "NORMAL", "HIGH", "VERY_HIGH"]
  },
  "emergencyCall": {
    "enabled": true
  },
  "battery": {
    "lowBatteryEnabled": true,
    "thresholdPercent": 25,
    "thresholdOptions": [10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100]
  }
}
```

## Confirm Before Implementation

### Planner / PM

- Topic: 배터리 임계값 옵션
- Current plan: 배터리 임계값은 10%부터 100%까지 5% 단위 단일 선택으로 저장한다.
- Need to confirm: 10~100% 전체를 노출할지, 화면 시안처럼 일부 값만 강조하거나 추천값을 둘지 확인이 필요하다.
- Why: 옵션 정책이 API 응답 옵션과 QA 케이스에 반영된다.
- Recommended default: 10~100%, 5% 단위 전체 제공.
- Owner: PM
- Status: Blocking

### Planner / PM

- Topic: 초기 기본값
- Current plan: 설정 row가 없으면 기본 설정을 반환하고, 첫 수정 시 row를 생성한다.
- Need to confirm: 안심존/이상탐지/응급호출/배터리 알림의 초기 ON/OFF와 배터리 기본 임계값을 확정해야 한다.
- Why: 신규 대상자 또는 기존 대상자에게 설정 row가 없는 경우 앱이 표시할 값이 필요하다.
- Recommended default: 전체 알림 ON, 이상탐지 민감도 `NORMAL`, 배터리 임계값 `25`.
- Owner: PM
- Status: Blocking

### Planner / PM

- Topic: 관계자 수정 권한
- Current plan: 주보호자, 관계자, 대상자 본인 모두 조회와 수정이 가능하다.
- Need to confirm: 관계자도 설정 변경까지 가능한 정책이 맞는지 최종 확인이 필요하다.
- Why: 관계자가 수정 가능하면 한 사용자의 변경이 모든 사용자에게 즉시 보이는 공통 설정이 된다.
- Recommended default: 모두 수정 가능.
- Owner: PM
- Status: Blocking

### Frontend / App

- Topic: GET API가 하나인 이유와 화면 진입 방식
- Current plan: 세부 설정 전체를 `GET /api/v1/notifications/settings/{wardKey}` 하나로 조회한다.
- Need to confirm: 앱이 ST-003 진입 시 한 번 조회한 값을 세부 화면에 전달하거나 캐시하고, 딥링크로 세부 화면에 바로 진입해도 같은 GET을 호출하는 방식으로 구현 가능한지 확인이 필요하다.
- Why: 화면별 GET API 4개를 만들면 호출 수와 API 계약이 늘어난다. 현재는 전체 설정 payload가 작아서 단일 GET이 단순하다.
- Recommended default: 단일 GET 유지.
- Owner: App
- Status: Follow-up

### Backend / QA

- Topic: 실제 푸시 발송 연동 제외
- Current plan: 이번 작업은 설정 저장/조회 API까지만 구현하고, 실제 알림 발송 필터링은 후속 작업으로 분리한다.
- Need to confirm: QA가 이번 범위에서 푸시 수신 여부 변경까지 검증하지 않도록 범위를 명확히 해야 한다.
- Why: 저장값은 생기지만 아직 발송 로직에는 반영되지 않는 상태가 될 수 있다.
- Recommended default: 이번 QA 범위는 API 저장/조회와 앱 표시 반영까지만 검증.
- Owner: Backend, QA, PM
- Status: Blocking

## Recommended Defaults If No Objection

- `GET /api/v1/notifications/settings/{wardKey}` 하나로 모든 세부 설정을 조회한다.
- 카테고리별 `PATCH` API 4개를 제공한다.
- 설정은 `wardKey` 기준으로 하나만 저장한다.
- 주보호자, 관계자, 대상자 본인 모두 조회/수정 가능하다.
- 이상탐지 민감도는 `VERY_LOW`, `LOW`, `NORMAL`, `HIGH`, `VERY_HIGH` 5단계로 저장한다.
- 배터리 임계값은 10~100%, 5% 단위 단일 선택으로 저장한다.
- 접근금지구역은 이번 범위에서 제외한다.
- 실제 푸시 발송 필터링 연동은 후속 작업으로 분리한다.

## Notes For Spec Update

- `docs/기능명세서.md`의 ST-003 ~ ST-003-4에 저장/조회 API 범위와 즉시 저장 정책을 반영한다.
- 배터리 임계값 기본값과 옵션 정책을 명세에 명시한다.
- 이상탐지 민감도 5단계의 코드값과 화면 라벨 매핑을 명세 또는 API 문서에 명시한다.
- 접근금지구역은 이번 범위 제외라고 명세 또는 티켓에 남긴다.
