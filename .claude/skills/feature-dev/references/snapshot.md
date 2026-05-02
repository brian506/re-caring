# 프로젝트 스냅샷

> 마지막 업데이트: 2026-04-25. 기능 추가·수정 시 해당 섹션을 갱신한다.

## 도메인별 패키지 현황

| 도메인 | 주요 Service | 주요 Implement |
|--------|-------------|---------------|
| `auth` | LocalAuthService, OAuthService, TokenRefreshService | LocalAuthAuthenticator, TokenIssuer, RefreshTokenReader/Writer, OAuthManager |
| `care` | CareInvitationService, CareRelationshipService | CareInvitationManager, CareInvitationReader/Writer, CareRelationshipValidator, SqsPublisher(전략패턴) |
| `device` | DeviceTokenService | — (Repository 직접 사용) |
| `location` | LocationService | GpsHistoryReader/Writer, GpsLatestCacheReader/Writer, SseEmitterManager, LocationValidator |
| `member` | MemberService | MemberReader/Writer/Validator |
| `sms` | PhoneVerificationService | SmsClient, SmsCodeGenerator, PhoneVerificationReader/Writer |

## API 엔드포인트

| 도메인 | Method | Path | 설명 |
|--------|--------|------|------|
| Auth | POST | `/api/v1/auth/sign-up` | 로컬 회원가입 |
| Auth | POST | `/api/v1/auth/sign-in` | 로컬 로그인 |
| Auth | POST | `/api/v1/auth/oauth/sign-in` | OAuth 로그인 (카카오/네이버) |
| Auth | POST | `/api/v1/auth/oauth/sign-up` | OAuth 회원가입 |
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
| Device | POST | `/api/v1/device/token` | Device Token 발급 (WARD, JWT 인증) |
| Location | POST | `/api/v1/location/gps` | GPS 좌표 전송 (WARD, Device Token 인증) |
| Location | GET | `/api/v1/location/stream/{wardKey}` | SSE 실시간 위치 스트림 (GUARDIAN) |
| Location | GET | `/api/v1/location/history/{wardKey}` | 날짜별 이동 경로 히스토리 |
| Member | GET/PATCH | `/api/v1/member/...` | 회원 정보 조회/수정 |
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
| GpsHistory | gps_histories | wardMemberKey, latitude, longitude, recordedAt |
| WardDeviceToken | ward_device_tokens | wardKey(UUID, UNIQUE), token(UUID, UNIQUE), createdAt, expiresAt |
| MembersTermsAgreement | members_terms_agreements | memberKey, agreedAt |

## Redis 키 구조

```
refresh:{memberKey}       RefreshToken          TTL: 7일
sms:{phone}               SMS 인증코드          TTL: 3분
gps:latest:{memberKey}    GPS 최신 위치         TTL: 5분  { lat, lng, timestamp }
```

## SqsPublisher 전략 패턴

- `SqsPublisher` (interface) — care.implement
- `AwsSqsPublisher` (AWS SQS 실제 구현, prod 프로파일)
- `NoOpSqsPublisher` (로컬/테스트용 no-op, local/test 프로파일)

---

## 알려진 기술 부채

> 개선 구현 시 해당 항목을 제거한다.

| # | 항목 | 위치 | 설명 |
|---|------|------|------|
| 1 | SSE emitter 메모리 누수 위험 | `SseEmitterManager` | 부하 시 emitter 미제거 → heap 증가. broadcast 중 IOException 외 예외 케이스 누락 가능 |
| 2 | AFTER_COMMIT 실패 무시 | `GpsEventHandler` | DB 저장 성공 → Redis/SSE 실패 시 재시도 없음. Outbox 패턴 미적용 |
| 3 | GPS 히스토리 인덱스 미실행 | `GpsHistory` entity | `ward_member_key + recorded_at` 복합 인덱스 TODO 주석만 있고 DDL 미실행 |
| 4 | SSE 구독자 수 무제한 | `SseEmitterManager` | wardKey당 emitter 수 제한 없음 |
| 5 | CareInvitation 만료 정리 배치 없음 | `CareInvitation` | PENDING 만료건 DB에 잔류, 주기적 정리 미구현 |
| 6 | Device Token 검증 DB 직조회 | `DeviceTokenAuthFilter` | GPS 수신마다 `WardDeviceTokenRepository.findByToken()` DB 조회. Redis 캐싱(`device-token:{token}` → wardMemberKey, TTL 24h)으로 개선 가능. 단, 재발급(`reissue()`) 시 구 토큰 캐시 명시적 삭제 필요 — 트러블슈팅 비교 후 적용 결정 |
