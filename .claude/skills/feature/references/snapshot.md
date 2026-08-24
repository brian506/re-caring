# 프로젝트 스냅샷

> 마지막 업데이트: 2026-08-13. 기능 추가·수정 시 해당 섹션을 갱신한다.

## 도메인별 패키지 현황

| 도메인 | 주요 Service | 주요 Implement |
|--------|-------------|---------------|
| `auth` | LocalAuthService, OAuthService, TokenRefreshService | LocalAuthAuthenticator, TokenIssuer, RefreshTokenReader/Writer, OAuthManager |
| `care` | CareInvitationService, CareRelationshipService | CareInvitationManager, CareInvitationReader/Writer, CareRelationshipValidator, SqsPublisher(전략패턴) |
| `device` | DeviceTokenService | WardDeviceTokenManager, WardDeviceTokenReader |
| `location` | LocationService, LocationSettingService | GpsHistoryManager, GpsLatestCacheManager/Listener, SseEmitterManager, LocationValidator, CareRelationshipCacheReader, BatteryThresholdEvaluator/BatteryDetectionListener/BatteryAlertStateManager(detection), SafeZoneStateManager/SafeZoneDetectionListener(safezone), LocationSettingManager |
| `member` | MemberService | MemberReader/Writer/Validator, MembersTermsAgreementWriter, MemberWithdrawalManager |
| `notification` | NotificationService, NotificationSettingService, FcmDeviceTokenService | NotificationReader/Writer, NotificationSendManager, NotificationSettingReader/Manager/Validator, FcmDeviceTokenReader/Manager, FcmClient(Firebase/NoOp), CareInvitationNotificationListener, BatteryThresholdNotificationListener, SafeZoneNotificationListener |
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
| Care | DELETE | `/api/v1/care/wards/{wardKey}` | 보호 대상자 케어 관계 삭제 (GUARDIAN, MANAGER) |
| Care | DELETE | `/api/v1/care/wards/{wardKey}/caregivers/{caregiverKey}` | 특정 보호자/관리자 케어 관계 삭제 (GUARDIAN only) |
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
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/anomaly` | 이상탐지 알림 토글·민감도 수정 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/emergency-call` | 응급호출 알림 토글 |
| Notification | PATCH | `/api/v1/notifications/settings/{wardKey}/battery` | 배터리 알림 토글 + 알림 받을 잔량(%) 다중 선택 (10~100, 10 단위, 개수 제한 없음). 기본값 없음 — 빈 배열이면 알림 안 감 |
| Member | GET | `/api/v1/members/me` | 내 정보 조회 (JWT 인증, Member+이메일+약관+안심존 통합) |
| Member | PATCH | `/api/v1/members/me` | 내 정보 수정 (이름·생년월일·비밀번호 부분 수정, JWT 인증) |
| Member | POST | `/api/v1/members/phones` | 연락처 기반 가입 회원 조회 (GUARDIAN) |
| Member | DELETE | `/api/v1/members/me` | 회원 탈퇴 |
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
| CareRelationship | care_relationships | caregiverKey, wardKey, role(GUARDIAN/MANAGER) |
| CareInvitation | care_invitations | inviterKey, receiverKey, wardKey, status(PENDING/ACCEPTED/REJECTED/EXPIRED), createdAt |
| GpsHistory | gps_histories | wardMemberKey, latitude, longitude, recordedAt(서버 수신 시각), accuracy, battery, speed(m/s, nullable), measuredAt(기기 측정 시각, nullable — null이면 시간 간격 신뢰 불가). 시각 컬럼은 모두 KST 저장 |
| LocationSetting | location_settings | wardMemberKey(UNIQUE), collectionIntervalSeconds(30/60/180/300, 기본 30) |
| WardDeviceToken | ward_device_tokens | wardKey(UUID, UNIQUE), token(UUID, UNIQUE), createdAt, expiresAt |
| MembersTermsAgreement | members_terms_agreements | memberKey, agreedAt |
| SafeZone | safe_zones | safeZoneKey(UUID), wardMemberKey, name, address, latitude, longitude, radius(SMALL/MEDIUM/LARGE/XLARGE) |
| SafeZoneState | safe_zone_states | wardMemberKey(UNIQUE), safeZoneKeys(CSV, 현재 속한 안심존). 행 없음=최초 관측(알림 안 함), 빈 문자열=존 밖 |
| Notification | notifications | notificationKey(UUID, UNIQUE), recipientMemberKey, eventType, title, body, dataPayload(jsonb, 리다이렉트용), createdAt. 수신자별 개별 row. 읽음 필드 없음 |
| NotificationSetting | notification_settings | wardMemberKey(UNIQUE), 안심존·이상탐지·응급호출 토글, 이상탐지 민감도, lowBatteryEnabled, batteryThresholdPercents(CSV, 기본 '' = 선택 없음 → 알림 없음) |
| AlertRunbook | alert_runbooks | errorSignature, commands(jsonb), resolutionContext, successCount, isValid |
| AlertInvestigation | alert_investigations | fingerprint, alertName, severity, threadTs, status, fixCommands(jsonb) |

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
이상이동 탐지(경로 이탈·배회·속도·시간대)는 향후 Claim-Check 방식(SQS는 트리거만, 엔진이 DB 조회)으로 붙인다.
→ 결정 배경과 미해결 항목: `docs/detection-architecture-decisions.md`

## SqsPublisher 전략 패턴

- `SqsPublisher` (interface) — common.sqs (care.implement에 미사용 중복 3형제가 남아 있음)
- `AwsSqsPublisher` (AWS SQS 실제 구현, prod/dev 프로파일)
- `NoOpSqsPublisher` (로컬/테스트용 no-op)
- 현재 사용처 없음 — 이상이동 탐지 경로 착수 시 재사용

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
