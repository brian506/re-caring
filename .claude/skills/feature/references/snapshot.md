# 프로젝트 스냅샷

> 마지막 업데이트: 2026-09-05. 기능 추가·수정 시 해당 섹션을 갱신한다.

## 도메인별 패키지 현황

| 도메인 | 주요 Service | 주요 Implement |
|--------|-------------|---------------|
| `auth` | LocalAuthService, OAuthService, TokenRefreshService | LocalAuthAuthenticator, TokenIssuer, RefreshTokenReader/Writer, OAuthManager |
| `care` | CareInvitationService, CareRelationshipService | CareInvitationManager, CareInvitationReader/Writer, CareRelationshipValidator |
| `device` | DeviceTokenService | WardDeviceTokenManager, WardDeviceTokenReader |
| `location` | LocationService, LocationSettingService | GpsHistoryManager, GpsLatestCacheManager/Listener, SseEmitterManager, LocationValidator, CareRelationshipCacheReader, BatteryThresholdEvaluator/BatteryDetectionListener/BatteryAlertStateManager/DetectionPublisher/DetectionListener/AnomalyDetectionConsumer/AnomalyDetectionParser/AnomalyDetectionManager(detection), SafeZoneStateManager/SafeZoneDetectionListener(safezone), LocationSettingManager |
| `member` | MemberService | MemberReader/Writer/Validator, MembersTermsAgreementWriter, MemberWithdrawalManager |
| `notification` | NotificationService, NotificationSettingService, FcmDeviceTokenService | NotificationReader/Writer, NotificationSendManager, NotificationSettingReader/Manager/Validator, FcmDeviceTokenReader/Manager, FcmClient(Firebase/NoOp), CareInvitationNotificationListener, BatteryThresholdNotificationListener, SafeZoneNotificationListener, AnomalyNotificationListener |
| `place` | PlaceService | KakaoPlaceSearchClient (카카오 로컬 키워드 검색 프록시, 엔티티 없음) |
| `safezone` | SafeZoneService | SafeZoneReader, SafeZoneWriter |
| `sms` | PhoneVerificationService | SmsClient, SmsCodeGenerator, PhoneVerificationReader/Writer |
| `alert` | AlertService, AlertResolutionService | AlertInvestigationOrchestrator, AlertInvestigationAgent(Tool Use Loop), SsmContextFetcher, PrometheusContextFetcher, ErrorHistoryFetcher, RunbookService, SlackAlertNotifier, GitHubPrCreator, AlertRetryHandler |

## API 엔드포인트

| 도메인 | Method | Path | 설명 |
|--------|--------|------|------|
| Auth | POST | `/api/v1/auth/sign-up` | 로컬 회원가입 |
| Auth | POST | `/api/v1/auth/sign-in` | 로컬 로그인 |
| Auth | POST | `/api/v1/auth/sign-in/{kakao\|naver}` | OAuth 로그인 (미연동 계정은 OAUTH_NOT_LINKED) |
| Auth | POST | `/api/v1/auth/oauth/link/{kakao\|naver}` | OAuth 사후 연동 (JWT 인증, 로컬 가입 필수) |
| Auth | POST | `/api/v1/auth/token/refresh` | 토큰 갱신 |
| Auth | GET | `/api/v1/auth/email/mask` | 이메일 마스킹 조회 |
| Auth | PATCH | `/api/v1/auth/password` | 비밀번호 변경 |
| Care | POST | `/api/v1/care/requests/ward` | 보호대상자 추가 요청 (GUARDIAN) |
| Care | POST | `/api/v1/care/requests/manager` | 관리자 추가 요청 |
| Care | POST | `/api/v1/care/requests/guardian` | 보호자 추가 요청 |
| Care | GET | `/api/v1/care/requests/received` | 받은 케어 요청 목록 |
| Care | PATCH | `/api/v1/care/requests/{key}/accept` | 케어 요청 수락 |
| Care | PATCH | `/api/v1/care/requests/{key}/reject` | 케어 요청 거절 |
| Care | GET | `/api/v1/care/wards` | 내 보호대상자 목록 |
| Care | GET | `/api/v1/care/wards/{wardKey}/caregivers` | 보호자/관리자 목록 |
| Care | DELETE | `/api/v1/care/wards/{wardKey}` | 보호 대상자 케어 관계 삭제 (케어 관계가 있는 회원 전원. **주보호자가 삭제하면 그 대상자의 케어 관계 전체가 함께 해제됨**) |
| Care | DELETE | `/api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}` | 특정 보호자/관계자 케어 관계 삭제 (PRIMARY_GUARDIAN only) |
| Care | PATCH | `/api/v1/care/wards/{wardKey}/nickname` | 보호 대상자 별명 수정 (케어 관계가 있는 회원 전원, 보호자별로 따로 보임. 빈 값이면 해제) |
| Care | PATCH | `/api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}/role` | 보호자/관계자 관계 수정 (PRIMARY_GUARDIAN only, GUARDIAN↔MANAGER만) |
| Device | POST | `/api/v1/device/token` | Device Token 발급 (WARD, JWT 인증) |
| Location | POST | `/api/v1/location/gps` | GPS 좌표 전송 (WARD, Device Token 인증) |
| Location | GET | `/api/v1/location/stream/{wardKey}` | SSE 실시간 위치 스트림 (GUARDIAN) |
| Location | GET | `/api/v1/location/history/{wardKey}` | 날짜별 이동 경로 히스토리 |
| Location | GET | `/api/v1/location/settings/{wardKey}/collection-interval` | 위치 수집 주기 조회 (GUARDIAN, 옵션 30/60/180/300초 포함) |
| Location | PATCH | `/api/v1/location/settings/{wardKey}/collection-interval` | 위치 수집 주기 수정 (GUARDIAN only) |
| Location | GET | `/api/v1/location/settings/collection-interval/me` | 내 위치 수집 주기 조회 (WARD, Device Token 인증) |
| Notification | GET | `/api/v1/notifications` | 내 알림함 목록 조회 (WARD, GUARDIAN — recipient 기준, 최신순, 페이징 없음) |
| Notification | GET | `/api/v1/notifications/settings/{wardKey}` | 알림 설정 조회 (안심존·이상탐지·응급호출·배터리) |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/safe-zone` | 안심존 진입·이탈 알림 토글 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/anomaly` | 이상탐지 알림 토글 수정 (5종 각각 on/off. **민감도 제거됨**) |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/emergency-call` | 응급호출 알림 토글 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/battery` | 배터리 알림 토글 + 알림 받을 잔량(%) 다중 선택 (10~100, 10 단위, 개수 제한 없음). 기본값 없음 — 빈 배열이면 알림 안 감 |
| Member | GET | `/api/v1/members/me` | 내 정보 조회 (JWT 인증, Member+이메일+약관+안심존 통합) |
| Member | PATCH | `/api/v1/members/me` | 내 정보 수정 (이름·생년월일·비밀번호 부분 수정, JWT 인증) |
| Member | POST | `/api/v1/members/phones` | 연락처 기반 가입 회원 조회 (GUARDIAN) |
| Member | DELETE | `/api/v1/members/me` | 회원 탈퇴 |
| Place | GET | `/api/v1/places/search?query=&latitude=&longitude=&radiusMeters=` | 장소 검색 (GUARDIAN·WARD, 카카오 로컬 키워드 검색 프록시, 최대 5건, 편향 반경 결과 없으면 전국 재검색, 결과 없음은 빈 배열 200) |
| SafeZone | POST | `/api/v1/care/wards/{wardKey}/safe-zones` | 안심존 추가 (GUARDIAN only) |
| SafeZone | GET | `/api/v1/care/wards/{wardKey}/safe-zones` | 안심존 목록 조회 (GUARDIAN, MANAGER) |
| SafeZone | GET | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존 상세 조회 (GUARDIAN, MANAGER) |
| SafeZone | PATCH | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존 수정 (GUARDIAN only) |
| SafeZone | DELETE | `/api/v1/care/wards/{wardKey}/safe-zones/{safeZoneKey}` | 안심존 삭제 (GUARDIAN only) |
| SMS | POST | `/api/v1/sms/verification/send` | SMS 인증코드 발송 |
| SMS | POST | `/api/v1/sms/verification/verify` | SMS 인증코드 검증 |

## 엔티티 목록

| Entity | Table | 주요 필드 |
|--------|-------|---------|
| Member | members | memberKey(UUID), role(GUARDIAN/WARD), name, phone |
| LocalAuth | local_auths | account, encodedPassword, memberKey |
| OAuth | oauths | provider(KAKAO/NAVER), providerId, memberKey |
| LoginHistory | login_histories | memberKey, ip, loginAt |
| CareRelationship | care_relationships | caregiverKey, wardKey, role(PRIMARY_GUARDIAN/GUARDIAN/MANAGER), wardNickname(nullable, 보호자별 별명. null이면 대상자 실명 사용) |
| CareInvitation | care_invitation | inviterKey, receiverKey, wardKey, careRole(수락 시 부여될 역할. 대상자 추가 요청은 PRIMARY_GUARDIAN), status(PENDING/ACCEPTED/REJECTED/EXPIRED), createdAt |
| GpsHistory | gps_histories | wardMemberKey, latitude, longitude, recordedAt(서버 수신 시각), accuracy, battery, speed(m/s, nullable), measuredAt(기기 측정 시각, nullable — null이면 시간 간격 신뢰 불가). 시각 컬럼은 모두 KST 저장 |
| LocationSetting | location_settings | wardMemberKey(UNIQUE), collectionIntervalSeconds(30/60/180/300, 기본 30) |
| WardDeviceToken | ward_device_tokens | wardKey(UUID, UNIQUE), token(UUID, UNIQUE), createdAt, expiresAt |
| MembersTermsAgreement | members_terms_agreements | memberKey, agreedAt |
| SafeZone | safe_zones | safeZoneKey(UUID), wardMemberKey, name, address, latitude, longitude, radius(SMALL/MEDIUM/LARGE/XLARGE) |
| SafeZoneState | safe_zone_states | wardMemberKey(UNIQUE), safeZoneKeys(CSV, 현재 속한 안심존). 행 없음=최초 관측(알림 안 함), 빈 문자열=존 밖 |
| Notification | notifications | notificationKey(UUID, UNIQUE), recipientMemberKey, eventType, title, body, dataPayload(jsonb, 리다이렉트용), createdAt. 수신자별 개별 row. 읽음 필드 없음 |
| AnomalyDetection | anomaly_detections | wardMemberKey, detectionType(5종), score, detectedAt, latitude, longitude, evidence(1000자). `(wardMemberKey, detectionType, detectedAt)` UNIQUE = 재배달 멱등 키. 알림 토글과 무관하게 항상 저장되는 탐지 사건 원본 (1건 = 1 row) |
| NotificationSetting | notification_settings | wardMemberKey(UNIQUE), 안심존·응급호출 토글, 이상탐지 토글 5종(speed/wandering/abnormalDwelling/routeDeviation/timeAnomaly), lowBatteryEnabled, batteryThresholdPercents(CSV, 기본 '' = 선택 없음 → 알림 없음) |
| AlertRunbook | alert_runbooks | errorSignature, commands(jsonb), resolutionContext, successCount, isValid |
| AlertInvestigation | alert_investigations | fingerprint, alertName, severity, threadTs, status, fixCommands(jsonb) |

## 케어 역할(CareRole) 3단계

대상자를 등록한 사람이 `PRIMARY_GUARDIAN`(주보호자)이 되고, 그 사람만 다른 사람을 케어에 추가·삭제하거나
역할을 바꿀 수 있다. 이전에는 `GUARDIAN`/`MANAGER` 2단계였고 주보호자도 `GUARDIAN`이었는데,
`validateCanAddGuardian`이 GUARDIAN 한도 1을 검사해 **보호자 추가가 항상 실패**했다. 3단계로 나누며 함께 해소됐다.

| 기능 | PRIMARY_GUARDIAN | GUARDIAN | MANAGER |
|------|------|------|------|
| 보호자/관계자 추가 | O | X | X |
| 케어관계 삭제(타인) | O | X | X |
| 관계(역할) 변경 | O | X | X |
| 안심존 등록·수정·삭제 | O | O | X |
| 위치 수집주기 변경 | O | O | X |
| 위치 조회·SSE | O | O | O |
| 알림 수신·설정 | O | O | O |
| 별명 설정 | O | O | O |

한도: PRIMARY_GUARDIAN 1명, GUARDIAN 1명, MANAGER 3명. 보호자가 맡을 수 있는 대상자는 5명.

**주보호자 자리는 고정이다 — 넘길 수 없다.** 자기 역할을 바꿀 수도(E5015), 다른 사람을 주보호자로 올릴 수도(E5016),
삭제 대상이 될 수도(E5017) 없다. 그래서 주보호자가 그냥 떠나면 남은 보호자·관계자를 정리할 주체가 영영 사라진다.
`removeWard`에서 **주보호자가 나가면 그 대상자의 케어 관계를 통째로 삭제**하는 이유다. 남은 사람들은 모두
주보호자가 초대해 들어온 사람들이라 의미상으로도 맞다. JPA 연관관계·cascade는 쓰지 않는다 —
`CareRelationshipManager.leaveCare`가 역할을 보고 분기해 `ward_member_key` 단일 조건 벌크 삭제를 날린다.
클라이언트는 삭제 전 "연결된 N명도 함께 해제됩니다" 확인을 받아야 한다.

**케어 관계와 함께 `care_invitation`도 지운다.** 초대에는 만료가 없어서(`expiredAt` 없음, `EXPIRED` 사용처 0건)
남겨두면 뒤늦은 수락으로 관계가 되살아난다.

**주보호자가 없는 대상자에 맺어지는 관계는 역할과 무관하게 주보호자가 된다**(`CareRelationshipWriter.resolveCareRole`).
관계자로 들어오게 두면 그를 내보낼 주체가 없는 대상자가 만들어진다 — `removeCaregiver`가 주보호자를 요구하기 때문이다.
초대 정리와 이 승격은 같은 구멍을 앞뒤로 막는다: 정리가 새 관계의 유입을 줄이고, 승격이 그래도 들어온 관계를 안전하게 만든다.

**케어 관계가 끊겨도 대상자 데이터는 남는다** — `safe_zones`·`location_settings`·`notification_settings`·
`ward_device_tokens`·`gps_histories`·`anomaly_detections`. **의도한 동작이다.** 새 주보호자가 등록되면 이전 설정과
이력을 그대로 물려받는다. 대상자를 기준으로 쌓인 데이터라 보호자가 바뀐다고 초기화할 이유가 없다는 판단.

안심존 단건 조회·수정·삭제는 `(safeZoneKey, wardMemberKey)` 쌍으로 스코프한다. `safeZoneKey`만으로 찾으면
경로의 `wardKey`로 인가를 통과한 뒤 **다른 대상자의 안심존**을 읽고 고치고 지울 수 있다 —
캐스케이드로 관계가 끊긴 전 보호자가 키를 기억하고 있으면 그대로 악용된다.

대상자당 주보호자가 1명이라는 불변식은 **발급(`sendWardInvitation`)·수락(`register`)·DB(부분 유니크 인덱스)**
세 겹으로 강제한다(E5018). 수락 시점 검사가 따로 필요한 이유는 PENDING 초대가 첫 주보호자보다 먼저
만들어졌을 수 있어서다. 이 검사를 `validateCanAddWard` 안에 넣으면 안 된다 — `register`가 GUARDIAN 계정의
**모든 수락 경로**에서 그 메서드를 재사용하므로, 보호자·관계자 추가 수락이 전부 막힌다(실제로 겪은 회귀).

**주의**: 보호자 계열 판정은 반드시 `CareRole.guardianRoles()` / `CareRole.isGuardian()`을 쓴다.
`== CareRole.GUARDIAN`으로 적으면 주보호자가 권한·알림 대상에서 조용히 빠진다 —
위치 SSE, 안심존 CRUD, 알림 수신자 분류가 모두 이 판정을 탄다.

별명은 `(보호자, 대상자)` 쌍마다 하나라 `care_relationships`의 UNIQUE 키와 카디널리티가 같다.
그래서 별도 테이블을 만들지 않고 `ward_nickname` 컬럼을 뒀다. 관계가 삭제되면 별명도 같이 사라진다.
알림 본문은 별명이 아니라 실명을 쓴다 — 발행 시점에 본문이 확정되는 구조라 수신자별 별명을 적용하려면
조회가 한 번 더 들어간다.

## Redis 키 구조

```
sms:{phone}                    SMS 인증코드          TTL: 3분
gps:latest:{memberKey}         GPS 최신 위치         TTL: 5분  { lat, lng, timestamp }
investigation:{fingerprint}    Alert 조사 상태       TTL: 10분  { threadTs, status, startedAt, fixCommands }
device:state:{memberKey}       기기 상태             TTL 없음   ONLINE | LOW_BATTERY | OFFLINE
device:battery:{memberKey}     배터리 재알림 억제(hash) TTL: 1일, 억제 해제·탈퇴 시 삭제  { lastNotifiedThreshold }
careRelationship::*            케어관계 캐시           TTL: 10분
```

안심존 상태는 Redis(`safezone:state:{wardKey}`)에서 `safe_zone_states` 테이블로 이전했다. 유실되면 복구 불가능한
알림 이력이라 TTL이 있는 저장소와 성격이 맞지 않았고, 저장 실패 시 알림만 나가는 경로가 있었다.
안심존 목록은 캐싱하지 않고 GPS 지점마다 DB를 조회한다(핫패스 아님 + 존 수정 시 오알림 방지).

## Redis 인프라 (ECS+EFS → ElastiCache 전환)

이상탐지 파이프라인(Redis Stream으로 실시간 GPS 전달) 도입을 계기로, EC2에 종속된 ECS 컨테이너 Redis(EFS AOF)를
ElastiCache로 전환했다. 기존엔 refresh token/SMS 코드/최신 위치 모두 유실 허용 데이터였지만(리프레시 토큰은
애초에 Redis가 아니라 DB(`RefreshTokenRepository`) 저장이라 대상 아님), 이상탐지 알림 전달 경로가 되면서
EC2 다운 시 탐지 흐름 자체가 멈추는 SPOF를 해소하기 위함.

**AWS 콘솔에 인스턴스 생성 완료** (2026-08-13), 코드/설정 반영은 아직.

| 항목 | 값 |
|------|-----|
| 클러스터 이름 | recaring-redis |
| 엔진 | Valkey 9.1.0 |
| 노드 유형 | cache.t3.micro |
| 클러스터 모드 | 비활성화 (샤드 1) |
| 노드 수 | 3 (Primary 1 + Replica 2) |
| Multi-AZ / 자동 장애조치 | 활성화 |
| 저장 시 암호화 | 활성화 |
| 전송 중 암호화(TLS) | 활성화, **모드: 필수** |
| 파라미터 그룹 | `recaring-parameter-policy` (커스텀) |
| Primary/Reader Endpoint | ElastiCache 콘솔 → `recaring-redis` 클러스터에서 확인 (리포지토리에 평문 기록하지 않음) |

**완료**
- `application-dev.yml`에 `spring.data.redis.ssl.enabled: true` 반영, ECS 태스크 정의 `REDIS_HOST`를 Primary Endpoint로 갱신
- 기존 ECS `redis`/`redis-exporter` 서비스 + EFS 볼륨(redis용) 제거

**남은 작업 (TODO)**
- AUTH 토큰 미사용 상태 — SG가 앱 서버 SG로만 제한돼 있어 dev는 진행 가능하나, prod 반영 전 AUTH 토큰 적용 검토 (SEC-001)
- `recaring-parameter-policy`에 `maxmemory-policy`를 `volatile-lru`(또는 `noeviction`)로 설정했는지 확인 — Redis Stream(TTL 없는 키) 데이터가 메모리 압박 시 임의 삭제되지 않도록
- 위 반영 후 dev 배포하여 연결 검증

## 탐지 파이프라인

결정론 판정(배터리·안심존)은 룰엔진 왕복 없이 **서버 인라인**으로 처리한다. GPS 수신 → `GpsSavedEvent` →
`BatteryDetectionListener` / `SafeZoneDetectionListener`가 각각 판정하고, 도달·전이 시에만 알림 이벤트를 발행한다.

이상이동 탐지(경로 이탈·배회·속도·시간대)는 **Redis Stream**으로 별도 탐지 엔진 컨테이너에 넘긴다.
탐지 컨테이너는 상시 기동해 스트림을 계속 소비한다. SQS는 쓰지 않는다(코드·의존성 모두 제거 완료).

**탐지와 학습은 다른 트리거로 돈다.** 아래 스트림 경로는 *탐지* 전용이다.
학습 컨테이너(`recaring-detection-train`)는 실시간 좌표가 아니라 RDS에 쌓인 이력을 배치로 읽어
사용자별 모델을 S3에 쓰며, **EventBridge 스케줄로 기동**한다. 두 경로를 섞지 않는다.

```
GpsSavedEvent → DetectionListener → DetectionPublisher → XADD gps-detection → 탐지 엔진
                                                          ← XADD anomaly-alerts ← (후속: DetectionResultConsumer)
```

**`gps-detection` 발행 필드** (연동 명세 v2 기준. Redis Stream이라 값은 전부 문자열)

> 스트림 이름은 명세서의 `gps-coordinates`가 아니라 **`gps-detection`**이다(의도적 변경).
> 이름이 어긋나면 에러 없이 조용히 전달만 끊기므로, 엔진 쪽 `XREADGROUP` 대상도 반드시 같아야 한다.

| 필드 | 필수 | 출처 |
|------|------|------|
| `ward_member_key` | O | `GpsSavedEvent.memberKey` |
| `latitude` / `longitude` | O | `Gps` (WGS84 십진도) |
| `recorded_at` | O | `Gps.recordedAt` — `yyyy-MM-dd HH:mm:ss` (KST, 오프셋 없음) |
| `accuracy` | X | 미터. 100m 초과분 제외는 엔진 책임 |
| `battery` | X | 0~100 |

- `recorded_at`은 **서버 수신 시각**이다. 엔진이 학습에 RDS `gps_histories`를, 탐지에 이 스트림을 쓰므로 두 경로의
  시각이 같은 의미여야 한다. `measured_at`(기기 측정)은 nullable이라 건마다 기준이 달라져 쓰지 않는다.
  같은 이유로 시각에 오프셋을 붙이지 않는다 — RDS `timestamp` 표기와 통일.
- 정확도가 낮은 좌표도 거르지 않고 그대로 발행한다. 판정 제외는 엔진이 `accuracy`를 보고 정한다.
  (인라인 판정인 `SafeZoneDetectionListener`는 거르므로 기준이 다르다)
- null인 선택 필드는 빈 문자열이 아니라 맵에서 제외한다.
**전달 보장은 구간별로 다르다. 뭉뚱그리면 안 된다.**

| 구간 | 보장 | 근거 |
|------|------|------|
| 백엔드 → Redis (`XADD`) | **at-most-once** | 우리 선택. Redis 장애가 GPS 수신 API를 실패시키면 안 되므로 `@Async` 리스너에서 `DataAccessException`을 삼키고 warn만 남긴다. 재시도하지 않으므로 유실 가능 |
| Redis → 엔진 (`XREADGROUP`+`XACK`) | **at-least-once** | Redis Stream 기본 동작. 미ACK 메시지가 PEL(Pending Entries List)에 남아 `XAUTOCLAIM`으로 회수된다 |

- 발행 유실분은 RDS `gps_histories`에 남아 있으므로 학습·재계산으로 메운다. 실시간 판정만 그 좌표를 못 본다.
- **trim은 시간 기준(`MINID ~`, 1시간)이다.** 길이 기준(`MAXLEN`)이 아니다.
  이상탐지 알림은 실시간성이 목적이라 1시간 지나 판정된 좌표는 값이 없다 — 보관할 이유 자체가 없다.
  길이 기준이면 WARD 수가 늘수록 보존 창이 반비례해 줄지만(100명이면 8시간, 1,000명이면 50분),
  시간 기준은 WARD 수와 무관하게 1시간으로 고정된다. 그만큼 메모리 상한은 없어지지만 창이 1시간이라
  WARD 1,000명에서도 약 24MB 수준이라 t3.micro(568MB)에 부담이 안 된다.
- **trim은 PEL을 존중하지 않는다.** 아직 ACK되지 않은 메시지도 잘려나간다.
  MINID를 쓰면 이게 결함이 아니라 **의도한 동작**이다 — 1시간 넘게 미처리된 좌표는 버리는 게 맞다.
  단, 그래서 **at-least-once 보장 창도 정확히 1시간**이다. 엔진이 1시간 넘게 죽어 있으면 그 구간은 복구되지 않는다.
- **발행 실패는 재시도·DLQ 없이 삼킨다.** 좌표 원본은 이미 RDS에 저장된 뒤라 유실되는 건 데이터가 아니라
  실시간 판정 기회 한 건뿐이고, 30초 뒤 다음 좌표가 거의 같은 정보를 준다. 늦게 밀어넣는 건 1시간 보존 정책과도 모순.
  Redis가 죽어 XADD가 실패한 상황이라 DLQ를 Redis에 둘 수도 없다.
- 지금은 **`DataAccessException`을 한 덩어리로 잡아 warn 로그만 남긴다.** 원인별 예외 분리와 실패 메트릭은
  검토했으나 **의도적으로 보류**했다 — 운영 로그를 먼저 쌓아 실제로 어떤 실패가 나는지 본 뒤 필요한 것만 넣는다.
  (검토 내용은 이슈 #187 코멘트 참고)
- MINID는 앱 서버 시계로 계산하지만 Stream ID는 Redis 서버 시계로 찍힌다. 같은 VPC에서 둘 다 NTP 동기화되므로
  실무상 무시할 수준이나, 시계가 크게 어긋나면 trim 창이 함께 어긋난다.
- 소비자 그룹(`XGROUP CREATE`) 생성은 엔진 컨테이너 책임. 백엔드는 producer만 담당한다.
- **파티션 개념이 없다 — 단, 컨슈머를 늘릴 때만 문제가 된다.** Kafka처럼 키로 파티셔닝할 수 없어서
  consumer group에 컨슈머가 2개 이상이면 같은 ward의 좌표가 흩어지고, 한쪽이 잠깐 밀리면 **처리 순서가 뒤집힌다**
  (쌓이는 순서는 멀쩡하다 — 뒤집히는 건 처리 순서다). 속도 계산의 시간차가 음수가 되는 식으로 없는 이상을 만들어낸다.
  유실이 아니라서 다음 좌표가 와도 교정되지 않는다.

  **컨슈머 1개면 발생하지 않으며, 당분간 늘릴 이유도 없다.** 명세의 1.65ms/건으로 계산하면 초당 약 606건,
  WARD 1명이 30초당 1건이므로 컨슈머 1개로 **약 18,000명**까지 소화한다.

  여기서 "컨슈머"는 컨테이너가 아니라 `XREADGROUP GROUP <group> <consumer-name>`에 넘기는 **이름**이다.
  컨테이너와 1:1이 되는 건 그렇게 짜기 때문이고, 한 컨테이너가 워커를 여러 개 띄우면(파이썬 멀티프로세싱 등)
  컨테이너 1개여도 컨슈머는 N개다. Kafka와 달리 파티션 상한이 없어 이름만 다르면 얼마든지 늘어난다.
  (스케일 아웃 시 이름은 반드시 달라야 한다 — 같은 이름을 두 프로세스가 쓰면 PEL이 섞인다)
  → 처리량 때문에 컨슈머를 늘리는 시점에 ward 단위 재정렬 또는 `recorded_at` 기준 정렬이 필요해진다. 그전엔 무관.
- `is_charging`은 수집하지 않으므로 전송하지 않는다.

### `anomaly-alerts` — 엔진 → 백엔드 (백엔드가 컨슈머)

엔진이 이상을 판정하면 **사건당 1건** 발행한다(30초 주기 아님). 백엔드는 `XREADGROUP`으로 소비하고
알림을 저장한 뒤 `XACK`한다. 그룹 `recaring-backend`.

| 필드 | 설명 |
|------|------|
| `ward_member_key` | 입력의 `ward_member_key` 그대로 (필드명도 동일 — `user_id` 아님) |
| `detection_type` | 5종 — `SPEED_ANOMALY` / `WANDERING` / `ABNORMAL_DWELLING` / `ROUTE_DEVIATION` / `TIME_ANOMALY` |
| `score` | 항상 0.5000~1.0000, 소수 4자리 |
| `detected_at` | `yyyy-MM-dd HH:mm:ss` (KST, 오프셋 없음) |
| `latitude` / `longitude` | 소수 6자리 |
| `evidence` | 보호자에게 그대로 보여줄 한국어 한 문장. 엔진이 고정 템플릿에 숫자만 채워 만든다 |

**명세서 v2와 다른 점**: 명세의 `SIGNAL_LOST`는 채택하지 않았다(5종만). 다만 엔진이 보낼 가능성이 남아 있어
파서가 모르는 유형을 만나면 예외 대신 빈 결과를 주고, 컨슈머가 그 메시지를 버린 뒤 ACK한다.

**흐름**
```
AnomalyDetectionConsumer(수동 ACK)
  → AnomalyDetectionParser         파싱 실패 시 Optional.empty
  → AnomalyDetectionManager.record(@Transactional)
       INSERT ... ON CONFLICT DO NOTHING   중복(0)이면 여기서 종료
       → AnomalyDetectedEvent 동기 발행 (@Async 아님 — 저장 후 ACK해야 하므로)
            → AnomalyNotificationListener  토글 확인 → 보호자 조회 → 저장 + FCM
  → XACK
```

**탐지 기록과 알림은 분리돼 있다.** `anomaly_detections`는 알림 토글과 무관하게 항상 쌓이고,
토글은 `AnomalyNotificationListener`에서만 본다. 예전엔 토글이 꺼져 있으면 탐지 사실 자체가
로그 외에 아무데도 남지 않았다 — 모델 평가·튜닝의 근거가 사용자 설정에 좌우되면 안 된다.

- **ACK는 알림 저장 뒤에 한다.** 저장 전에 ACK하면 처리 중 죽었을 때 알림이 통째로 사라진다.
  `FirebaseFcmClient`는 예외를 내부에서 삼키므로(무효 토큰 목록만 반환) FCM 실패가 ACK를 막지 않는다.
- **알림 리스너에 `@Async`를 붙이지 않는다.** 다른 알림 리스너와 다른 점이다. 컨슈머 스레드에서 동기로 끝나야
  실패가 전파되어 ACK를 건너뛸 수 있다.
- **처리 못 하는 메시지는 버리고 ACK한다.** 알 수 없는 유형, 숫자·시각 형식 오류, 필수 필드 누락 모두.
  ACK하지 않으면 PEL에 남아 영원히 재배달되며 뒤가 밀린다(poison message).
- **`evidence`는 1000자로 자른다.** `notifications.body`가 `length=1000`이라 초과 시 INSERT가 실패하고,
  실패하면 ACK되지 않아 같은 poison 경로로 들어간다.
- **컨슈머 그룹은 백엔드가 만든다.** `createGroup`이 스트림까지 함께 만들어(MKSTREAM) 엔진이 첫 결과를 넣기 전에
  기동해도 된다. **`BUSYGROUP`은 정상으로 취급해야 한다** — 재기동·다중 태스크마다 발생하므로 던지면
  두 번째 배포부터 기동이 막힌다. (`RedisSystemException`으로 오며 `InvalidDataAccessApiUsageException`이 아니다)
- 컨슈머 이름은 hostname이다. 태스크마다 달라야 PEL이 섞이지 않는다.
- **중복 재배달은 `anomaly_detections`의 UNIQUE 제약으로 막는다.** ACK 전에 죽어 재배달되면 INSERT가 0을 돌려주고
  이벤트를 발행하지 않아 알림도 다시 나가지 않는다.
- **저장과 알림은 반드시 한 트랜잭션이어야 한다.** 알림 저장이 실패했는데 탐지 저장만 커밋되면,
  재배달분이 중복으로 걸러져 알림이 영영 발송되지 않는다. `AnomalyDetectionManager.record`의 `@Transactional`이
  그 경계이며, 실패 시 통째로 롤백되고 ACK도 건너뛰어 다음 재배달에서 처음부터 다시 처리된다.
- FCM 발송은 롤백되지 않는다. 다만 `NotificationSendManager`가 DB 저장을 먼저, FCM을 나중에 하므로
  DB가 실패하면 FCM 이전에 멈춘다.

---

## 알려진 기술 부채

> 개선 구현 시 해당 항목을 제거한다.

| # | 항목 | 위치 | 설명 |
|---|------|------|------|
| 1 | SSE emitter 메모리 누수 위험 | `SseEmitterManager` | 부하 시 emitter 미제거 → heap 증가. broadcast 중 IOException 외 예외 케이스 누락 가능 |
| 2 | AFTER_COMMIT 실패 무시 | `GpsEventHandler` | DB 저장 성공 → Redis/SSE 실패 시 재시도 없음. Outbox 패턴 미적용 |
| 3 | GPS 히스토리 인덱스 미실행 + 보관 정책 부재 | `GpsHistory` entity | `ward_member_key + recorded_at` 복합 인덱스 TODO 주석만 있고 DDL 미실행 → 모든 경로 조회가 seq scan. 보관 정리 수단도 없어 gp3 20GB가 실질 한도(WARD 1,000명이면 약 36일). 결정·검증 계획 → `docs/gps-storage-decisions.md` |
| 4 | SSE 구독자 수 무제한 | `SseEmitterManager` | wardKey당 emitter 수 제한 없음 |
| 5 | CareInvitation 만료 정리 배치 없음 | `CareInvitation` | PENDING 만료건 DB에 잔류, 주기적 정리 미구현 |
| 6 | Device Token 검증 DB 직조회 | `DeviceTokenAuthFilter` | GPS 수신마다 `WardDeviceTokenRepository.findByToken()` DB 조회. Redis 캐싱(`device-token:{token}` → wardMemberKey, TTL 24h)으로 개선 가능. 단, 재발급(`reissue()`) 시 구 토큰 캐시 명시적 삭제 필요 — 트러블슈팅 비교 후 적용 결정 |
| 7 | 안심존 설정 변경 시 가짜 진입·이탈 알림 | `SafeZoneDetectionListener` | `previousKeys`는 옛 존 설정 기준, `currentKeys`는 새 설정 기준이라 차집합의 전제가 깨진다. 존 신규 등록·반경 확대/축소·위치 이동 시 이동 없이 알림 발생. `zone == null` 체크는 삭제만 방어. 해결안: `SafeZoneChangedEvent` 발행 → 상태 리셋 |
| 8 | 겹친 안심존 동시 알림 | `SafeZoneDetectionListener` | 반경 500~2000m라 도심에서 존이 겹치면 교집합 진입·이탈 시 알림이 존 개수만큼 발송된다. 해결안: 판정 1회당 이벤트 1건으로 병합("집, 병원에 도착했어요"). FCM data payload의 `safeZoneKey` 복수화가 필요해 앱 팀 확인 선행 |
| 9 | 안심존 상태 동시 갱신 미방어 | `SafeZoneStateManager` | `findByWardMemberKey` → `replace`가 원자적이지 않다. GPS 최소 주기 30초라 정상 흐름에선 겹치지 않으나, 앱이 오프라인 버퍼링 후 일괄 전송하거나 재시도하면 중복 알림 가능. 실제 도착 간격 확인 후 필요 시 비관적 락 + `observed_at` 도입 |
| 10 | 장소 검색 캐시·호출 제한 없음 | `KakaoPlaceSearchClient` | 프록시에 캐시도 회원별 제한도 없다. 앱이 타이핑마다 호출하면 카카오 일일 쿼터가 소진되고 그 시점부터 429 → `PLACE_SEARCH_RATE_LIMITED`로 검색 기능 전체 정지. 개선안: `query` + 반올림 좌표 Redis 캐시(10분) → 회원별 분당 호출 제한 |
| 11 | 편향 폴백이 카카오 호출을 2배로 만든다 | `PlaceService` | 카카오의 `x/y/radius`는 우선순위가 아니라 필터라, 편향 반경 밖 장소는 검색이 안 되거나 엉뚱한 결과가 1~2건 섞여 온다(`해운대역` + 서울 편향 → '해운대연탄생갈비 부평갈산역점'). 그래서 "결과 0건"이 아니라 "검색어 토큰이 결과 이름에 하나도 안 걸림"을 폴백 조건으로 쓴다. 대신 편향 밖 검색마다 호출 2회 + 지연 최대 2배(읽기 타임아웃 3초 × 2). 이름 기반 휴리스틱이라 주소 문자열 검색(`서울 마포구 …`)은 항상 폴백을 탄다 |
